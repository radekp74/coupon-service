package pl.radoslawpiatek.couponservice.geolocation.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.util.Arrays;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.geolocation.adapters.IpWhoisGeoLocationResolver;
import pl.radoslawpiatek.couponservice.geolocation.adapters.PublicIpAddressPolicy;
import pl.radoslawpiatek.couponservice.geolocation.adapters.ServletClientIpResolver;
import pl.radoslawpiatek.couponservice.geolocation.adapters.StubGeoLocationResolver;
import pl.radoslawpiatek.couponservice.geolocation.ports.ClientIpResolver;
import pl.radoslawpiatek.couponservice.geolocation.ports.GeoLocationResolver;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics;

/**
 * Wires the trusted-client-IP and provider-neutral GeoIP adapters from validated configuration.
 *
 * <p>The ipwho.is adapter performs no I/O during bean creation. The deterministic stub can only be
 * selected when {@code local} or {@code test} is active; all other profiles fail startup instead of
 * silently weakening production behaviour.
 */
@Configuration
@EnableConfigurationProperties({ClientIpProperties.class, GeolocationProperties.class})
public class GeolocationConfiguration {

    /** Creates the Spring configuration; all runtime choices are made by the bean methods. */
    public GeolocationConfiguration() {
    }

    /**
     * Exposes the servlet trust-boundary adapter as the application client-IP port.
     *
     * @param properties validated client-IP settings
     * @param metrics low-cardinality client-IP metrics
     * @return resolver that is direct by default and bounded in trusted-proxy mode
     */
    @Bean
    ClientIpResolver clientIpResolver(ClientIpProperties properties, CouponServiceMetrics metrics) {
        return new ServletClientIpResolver(properties, metrics);
    }

    /**
     * Creates one HTTP client shared by all provider calls with redirects explicitly disabled.
     *
     * @param properties validated transport limits
     * @return shared HTTPS client with no automatic retry or redirect follow-up
     */
    @Bean
    HttpClient geolocationHttpClient(GeolocationProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * Chooses the configured provider without making a network request during application startup.
     *
     * @param properties validated provider settings
     * @param environment active Spring profiles used to guard the deterministic stub
     * @param httpClient shared HTTP client for the ipwho.is adapter
     * @param objectMapper Spring's configured JSON parser
     * @param metrics low-cardinality provider metrics
     * @return either the HTTPS provider adapter or the local/test-only stub
     */
    @Bean
    GeoLocationResolver geoLocationResolver(
            GeolocationProperties properties,
            Environment environment,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            CouponServiceMetrics metrics
    ) {
        if (properties.provider() == GeolocationProperties.Provider.IPWHOIS
                && !"https".equalsIgnoreCase(properties.baseUri().getScheme())) {
            throw new IllegalStateException("The ipwho.is provider requires an HTTPS base URI.");
        }
        if (properties.provider() == GeolocationProperties.Provider.STUB) {
            if (!hasLocalOrTestProfile(environment)) {
                throw new IllegalStateException("The geolocation stub is restricted to local or test profiles.");
            }
            return new StubGeoLocationResolver(CountryCode.of(properties.stubCountry()), metrics);
        }
        return new IpWhoisGeoLocationResolver(httpClient, objectMapper, properties, new PublicIpAddressPolicy(), metrics);
    }

    private boolean hasLocalOrTestProfile(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("local") || profile.equals("test"));
    }
}
