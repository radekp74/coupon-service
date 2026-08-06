package pl.radoslawpiatek.couponservice.geolocation.domain;

/**
 * Signals that a country cannot be obtained safely from the configured geolocation infrastructure.
 *
 * <p>The exception deliberately has no cause or message carrying an IP address, provider response
 * body, request URI or provider-specific diagnostic. A future public API maps it to stable 503
 * {@code GEOLOCATION_UNAVAILABLE}, not to a country-policy decision.
 */
public final class GeolocationUnavailableException extends RuntimeException {

    /** Creates the stable privacy-safe failure used for all provider and policy errors. */
    public GeolocationUnavailableException() {
        super("Geolocation is unavailable.");
    }
}
