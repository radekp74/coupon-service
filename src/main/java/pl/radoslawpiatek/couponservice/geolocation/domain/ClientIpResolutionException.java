package pl.radoslawpiatek.couponservice.geolocation.domain;

/**
 * Signals that the application cannot establish a trustworthy client address.
 *
 * <p>Its message intentionally contains no request header, address or proxy detail, because this
 * failure may later cross an HTTP boundary as the shared geolocation-unavailable error.
 */
public final class ClientIpResolutionException extends RuntimeException {

    /** Creates a privacy-safe infrastructure failure. */
    public ClientIpResolutionException() {
        super("Client address could not be resolved.");
    }
}
