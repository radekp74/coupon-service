package pl.radoslawpiatek.couponservice.geolocation.configuration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Validated provider configuration for memory-only country lookup.
 *
 * <p>The default is the HTTPS ipwho.is demonstration adapter. The stub is intentionally rejected
 * outside {@code local} and {@code test} by configuration, preventing an accidental production bypass.
 *
 * @param provider selected provider-neutral adapter
 * @param baseUri absolute HTTPS endpoint for the remote adapter
 * @param connectTimeout bounded time allowed to establish a remote connection
 * @param responseTimeout bounded time allowed for one remote response
 * @param maximumResponseBodyBytes maximum JSON body passed to the provider response parser
 * @param stubCountry deterministic ISO country returned only by the local/test stub
 */
@Validated
@ConfigurationProperties("coupon.geolocation")
public record GeolocationProperties(
        @NotNull Provider provider,
        @NotNull URI baseUri,
        @NotNull Duration connectTimeout,
        @NotNull Duration responseTimeout,
        @Min(1) @Max(16_384) int maximumResponseBodyBytes,
        @NotBlank String stubCountry
) {
    /** Supported provider adapters; unknown configuration values fail binding at startup. */
    public enum Provider {
        /** HTTPS adapter for the provider-neutral ipwho.is demonstration endpoint. */
        IPWHOIS,
        /** Deterministic country adapter limited to local and test profiles. */
        STUB
    }

    /**
     * Rejects invalid URL and timeout values before a request can be sent.
     *
     * @throws IllegalArgumentException when the HTTP adapter could violate the frozen transport limits
     */
    public GeolocationProperties {
        if (baseUri == null || baseUri.getScheme() == null) {
            throw new IllegalArgumentException("Geolocation base URI must be absolute.");
        }
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()
                || connectTimeout.compareTo(Duration.ofMillis(500)) > 0) {
            throw new IllegalArgumentException("Geolocation connect timeout must be between 1ms and 500ms.");
        }
        if (responseTimeout == null || responseTimeout.isNegative() || responseTimeout.isZero()
                || responseTimeout.compareTo(Duration.ofSeconds(1)) > 0) {
            throw new IllegalArgumentException("Geolocation response timeout must be between 1ms and 1s.");
        }
    }
}
