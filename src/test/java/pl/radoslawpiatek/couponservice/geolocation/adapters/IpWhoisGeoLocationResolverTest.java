package pl.radoslawpiatek.couponservice.geolocation.adapters;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.geolocation.configuration.GeolocationProperties;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;
import pl.radoslawpiatek.couponservice.geolocation.domain.GeolocationUnavailableException;

class IpWhoisGeoLocationResolverTest {

    private WireMockServer wireMock;

    @BeforeEach
    void startServer() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterEach
    void stopServer() {
        wireMock.stop();
    }

    @Test
    void resolvesCountryWithOneBoundedHttpsRequest() {
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(200).withBody("{\"success\":true,\"country_code\":\"PL\"}")));

        assertThat(resolver(Duration.ofSeconds(1)).resolve(ClientIpAddress.parseLiteral("8.8.8.8")))
                .isEqualTo(CountryCode.of("PL"));
        wireMock.verify(1, getRequestedFor(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Accept-Encoding", equalTo("identity")));
    }

    @Test
    void treatsProviderFailuresAsPrivacySafeUnavailable() {
        for (String body : new String[] {
                "{\"success\":false}",
                "{\"success\":\"true\",\"country_code\":\"PL\"}",
                "{\"country_code\":\"PL\"}",
                "{\"success\":true}",
                "{\"success\":true,\"country_code\":null}",
                "{\"success\":true,\"country_code\":123}",
                "{\"success\":true,\"country_code\":\"XXX\"}",
                "{\"success\":true,\"country_code\":\"ZZ\"}",
                "not-json"
        }) {
            wireMock.resetAll();
            wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                    .willReturn(aResponse().withStatus(200).withBody(body)));
            assertUnavailable(resolver(Duration.ofSeconds(1)));
        }
        for (int status : new int[] {400, 429, 500}) {
            wireMock.resetAll();
            wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                    .willReturn(aResponse().withStatus(status)));
            assertUnavailable(resolver(Duration.ofSeconds(1)));
        }
    }

    @Test
    void redirectDoesNotFollowLocationAndMakesExactlyOneRequest() {
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(302).withHeader("Location", "/target")));
        wireMock.stubFor(get(urlEqualTo("/target")).willReturn(aResponse().withStatus(200).withBody("{\"success\":true,\"country_code\":\"PL\"}")));

        assertUnavailable(resolver(Duration.ofSeconds(1)));
        wireMock.verify(1, getRequestedFor(urlEqualTo("/8.8.8.8?fields=success,country_code,message")));
        wireMock.verify(0, getRequestedFor(urlEqualTo("/target")));
    }

    @Test
    void rejectsOversizedDeclaredAndStreamingBodiesWithoutLeakage() {
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Length", "16385").withBody("x".repeat(16_385))));
        assertUnavailable(resolver(Duration.ofSeconds(1)));

        wireMock.resetAll();
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(200).withHeader("Transfer-Encoding", "chunked").withBody("x".repeat(16_385))));
        assertUnavailable(resolver(Duration.ofSeconds(1)));
    }

    @Test
    void acceptsExactlySixteenKiBBeforeJsonParsing() {
        String prefix = "{\"success\":true,\"country_code\":\"PL\",\"message\":\"";
        String body = prefix + "x".repeat(16_384 - prefix.length() - 2) + "\"}";
        assertThat(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(16_384);
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(200).withBody(body)));

        assertThat(resolver(Duration.ofSeconds(1)).resolve(ClientIpAddress.parseLiteral("8.8.8.8")))
                .isEqualTo(CountryCode.of("PL"));
    }

    @Test
    void responseTimeoutDoesNotRetryOrRevealAddress() {
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(250).withBody("{\"success\":true,\"country_code\":\"PL\"}")));

        assertUnavailable(resolver(Duration.ofMillis(100)));
        wireMock.verify(1, getRequestedFor(urlEqualTo("/8.8.8.8?fields=success,country_code,message")));
    }

    @Test
    void rejectsNonPublicAddressesBeforeAnyProviderRequest() {
        assertThatThrownBy(() -> resolver(Duration.ofSeconds(1)).resolve(ClientIpAddress.parseLiteral("10.0.0.7")))
                .isInstanceOf(GeolocationUnavailableException.class);
        wireMock.verify(0, getRequestedFor(com.github.tomakehurst.wiremock.client.WireMock.anyUrl()));
    }

    @Test
    void baseUriWithTrailingSlashProducesTheSameSingleRequestPath() {
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(200).withBody("{\"success\":true,\"country_code\":\"pl\"}")));

        GeolocationProperties properties = new GeolocationProperties(
                GeolocationProperties.Provider.IPWHOIS,
                URI.create("http://localhost:" + wireMock.port() + "/"),
                Duration.ofMillis(500), Duration.ofSeconds(1), 16_384, "PL"
        );
        IpWhoisGeoLocationResolver resolver = new IpWhoisGeoLocationResolver(
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500))
                        .followRedirects(HttpClient.Redirect.NEVER).build(),
                new ObjectMapper(), properties, new PublicIpAddressPolicy());

        assertThat(resolver.resolve(ClientIpAddress.parseLiteral("8.8.8.8"))).isEqualTo(CountryCode.of("PL"));
        wireMock.verify(1, getRequestedFor(urlEqualTo("/8.8.8.8?fields=success,country_code,message")));
    }

    private void assertUnavailable(IpWhoisGeoLocationResolver resolver) {
        assertThatThrownBy(() -> resolver.resolve(ClientIpAddress.parseLiteral("8.8.8.8")))
                .isInstanceOf(GeolocationUnavailableException.class)
                .hasMessage("Geolocation is unavailable.")
                .hasMessageNotContaining("8.8.8.8")
                .hasMessageNotContaining("x");
    }

    private IpWhoisGeoLocationResolver resolver(Duration responseTimeout) {
        GeolocationProperties properties = new GeolocationProperties(
                GeolocationProperties.Provider.IPWHOIS,
                URI.create("http://localhost:" + wireMock.port()),
                Duration.ofMillis(500), responseTimeout, 16_384, "PL"
        );
        return new IpWhoisGeoLocationResolver(
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500))
                        .followRedirects(HttpClient.Redirect.NEVER).build(),
                new ObjectMapper(), properties, new PublicIpAddressPolicy());
    }
}
