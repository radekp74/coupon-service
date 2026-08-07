package pl.radoslawpiatek.couponservice.geolocation.adapters;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.geolocation.configuration.GeolocationProperties;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;
import pl.radoslawpiatek.couponservice.geolocation.domain.GeolocationUnavailableException;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics;

class IpWhoisGeoLocationResolverTest {

    private static final Duration LOCAL_STUB_RESPONSE_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration LOCAL_TRANSPORT_WARMUP_TIMEOUT = Duration.ofSeconds(5);
    private static final HttpClient LOCAL_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(500))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private WireMockServer wireMock;

    @BeforeEach
    void startServer() throws Exception {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        warmUpLocalTransport();
    }

    @AfterEach
    void stopServer() {
        wireMock.stop();
    }

    @Test
    void resolvesCountryWithOneBoundedHttpsRequest() {
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(200).withBody("{\"success\":true,\"country_code\":\"PL\"}")));

        assertThat(resolver(LOCAL_STUB_RESPONSE_TIMEOUT).resolve(ClientIpAddress.parseLiteral("8.8.8.8")))
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
            assertUnavailable(resolver(LOCAL_STUB_RESPONSE_TIMEOUT));
        }
        for (int status : new int[] {400, 429, 500}) {
            wireMock.resetAll();
            wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                    .willReturn(aResponse().withStatus(status)));
            assertUnavailable(resolver(LOCAL_STUB_RESPONSE_TIMEOUT));
        }
    }

    @Test
    void redirectDoesNotFollowLocationAndMakesExactlyOneRequest() {
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(302).withHeader("Location", "/target")));
        wireMock.stubFor(get(urlEqualTo("/target")).willReturn(aResponse().withStatus(200).withBody("{\"success\":true,\"country_code\":\"PL\"}")));

        assertUnavailable(resolver(LOCAL_STUB_RESPONSE_TIMEOUT));
        wireMock.verify(1, getRequestedFor(urlEqualTo("/8.8.8.8?fields=success,country_code,message")));
        wireMock.verify(0, getRequestedFor(urlEqualTo("/target")));
    }

    @Test
    void rejectsOversizedDeclaredAndStreamingBodiesWithoutLeakage() {
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Length", "16385").withBody("x".repeat(16_385))));
        assertUnavailable(resolver(LOCAL_STUB_RESPONSE_TIMEOUT));

        wireMock.resetAll();
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(200).withHeader("Transfer-Encoding", "chunked").withBody("x".repeat(16_385))));
        assertUnavailable(resolver(LOCAL_STUB_RESPONSE_TIMEOUT));
    }

    @Test
    void acceptsExactlySixteenKiBBeforeJsonParsing() {
        String prefix = "{\"success\":true,\"country_code\":\"PL\",\"message\":\"";
        String body = prefix + "x".repeat(16_384 - prefix.length() - 2) + "\"}";
        assertThat(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(16_384);
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(200).withBody(body)));

        assertThat(resolver(LOCAL_STUB_RESPONSE_TIMEOUT).resolve(ClientIpAddress.parseLiteral("8.8.8.8")))
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
        assertThatThrownBy(() -> resolver(LOCAL_STUB_RESPONSE_TIMEOUT).resolve(ClientIpAddress.parseLiteral("10.0.0.7")))
                .isInstanceOf(GeolocationUnavailableException.class);
        wireMock.verify(0, getRequestedFor(com.github.tomakehurst.wiremock.client.WireMock.anyUrl()));
    }

    @Test
    void recordsExactBoundedProviderOutcomeMetrics() {
        SimpleMeterRegistry successRegistry = new SimpleMeterRegistry();
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(200).withBody("{\"success\":true,\"country_code\":\"PL\"}")));
        assertThat(resolver(LOCAL_STUB_RESPONSE_TIMEOUT, successRegistry)
                .resolve(ClientIpAddress.parseLiteral("8.8.8.8"))).isEqualTo(CountryCode.of("PL"));
        assertProviderMetric(successRegistry, "success", 1.0, 1L);

        wireMock.resetAll();
        SimpleMeterRegistry rateLimitedRegistry = new SimpleMeterRegistry();
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(429)));
        assertUnavailable(resolver(LOCAL_STUB_RESPONSE_TIMEOUT, rateLimitedRegistry));
        assertProviderMetric(rateLimitedRegistry, "rate_limited", 1.0, 1L);

        wireMock.resetAll();
        SimpleMeterRegistry invalidRegistry = new SimpleMeterRegistry();
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(200).withBody("not-json")));
        assertUnavailable(resolver(LOCAL_STUB_RESPONSE_TIMEOUT, invalidRegistry));
        assertProviderMetric(invalidRegistry, "invalid_response", 1.0, 1L);

        wireMock.resetAll();
        SimpleMeterRegistry providerErrorRegistry = new SimpleMeterRegistry();
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(500)));
        assertUnavailable(resolver(LOCAL_STUB_RESPONSE_TIMEOUT, providerErrorRegistry));
        assertProviderMetric(providerErrorRegistry, "provider_error", 1.0, 1L);

        wireMock.resetAll();
        SimpleMeterRegistry timeoutRegistry = new SimpleMeterRegistry();
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(250)
                        .withBody("{\"success\":true,\"country_code\":\"PL\"}")));
        assertUnavailable(resolver(Duration.ofMillis(100), timeoutRegistry));
        assertProviderMetric(timeoutRegistry, "timeout", 1.0, 1L);

        wireMock.resetAll();
        SimpleMeterRegistry nonPublicRegistry = new SimpleMeterRegistry();
        assertThatThrownBy(() -> resolver(LOCAL_STUB_RESPONSE_TIMEOUT, nonPublicRegistry)
                .resolve(ClientIpAddress.parseLiteral("10.0.0.7")))
                .isInstanceOf(GeolocationUnavailableException.class);
        assertThat(nonPublicRegistry.get("geolocation.resolution")
                .tags("provider", "ipwhois", "outcome", "non_public_ip").counter().count()).isEqualTo(1.0);
        assertThat(nonPublicRegistry.get("geolocation.provider")
                .tags("provider", "ipwhois", "outcome", "non_public_ip").timer().count()).isZero();
        wireMock.verify(0, getRequestedFor(com.github.tomakehurst.wiremock.client.WireMock.anyUrl()));
    }

    @Test
    void baseUriWithTrailingSlashProducesTheSameSingleRequestPath() {
        wireMock.stubFor(get(urlEqualTo("/8.8.8.8?fields=success,country_code,message"))
                .willReturn(aResponse().withStatus(200).withBody("{\"success\":true,\"country_code\":\"pl\"}")));

        GeolocationProperties properties = new GeolocationProperties(
                GeolocationProperties.Provider.IPWHOIS,
                URI.create("http://127.0.0.1:" + wireMock.port() + "/"),
                Duration.ofMillis(500), Duration.ofSeconds(1), 16_384, "PL"
        );
        IpWhoisGeoLocationResolver resolver = new IpWhoisGeoLocationResolver(
                LOCAL_HTTP_CLIENT, new ObjectMapper(), properties, new PublicIpAddressPolicy(),
                new CouponServiceMetrics(new SimpleMeterRegistry()));

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
        return resolver(responseTimeout, new SimpleMeterRegistry());
    }

    private IpWhoisGeoLocationResolver resolver(Duration responseTimeout, SimpleMeterRegistry registry) {
        GeolocationProperties properties = new GeolocationProperties(
                GeolocationProperties.Provider.IPWHOIS,
                URI.create("http://127.0.0.1:" + wireMock.port()),
                Duration.ofMillis(500), responseTimeout, 16_384, "PL"
        );
        return new IpWhoisGeoLocationResolver(
                LOCAL_HTTP_CLIENT, new ObjectMapper(), properties, new PublicIpAddressPolicy(),
                new CouponServiceMetrics(registry));
    }

    private void warmUpLocalTransport() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/__test-transport-warmup"))
                .willReturn(aResponse().withStatus(204)));
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + wireMock.port() + "/__test-transport-warmup"))
                .timeout(LOCAL_TRANSPORT_WARMUP_TIMEOUT)
                .GET()
                .build();
        HttpResponse<Void> response = LOCAL_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        assertThat(response.statusCode()).isEqualTo(204);
        wireMock.resetAll();
    }

    private void assertProviderMetric(SimpleMeterRegistry registry, String outcome, double counter, long timer) {
        assertThat(registry.get("geolocation.resolution")
                .tags("provider", "ipwhois", "outcome", outcome).counter().count()).isEqualTo(counter);
        assertThat(registry.get("geolocation.provider")
                .tags("provider", "ipwhois", "outcome", outcome).timer().count()).isEqualTo(timer);
    }
}
