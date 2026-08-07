package pl.radoslawpiatek.couponservice.geolocation.adapters;

import java.util.Objects;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;
import pl.radoslawpiatek.couponservice.geolocation.ports.GeoLocationResolver;

/**
 * Deterministic local/test resolver that never sends a client address across the network.
 *
 * <p>Construction is restricted by {@code GeolocationConfiguration}; this class itself contains no
 * profile switch so callers cannot turn a request header into a country-selection bypass.
 */
public final class StubGeoLocationResolver implements GeoLocationResolver {

    private final CountryCode countryCode;

    /**
     * Creates a resolver returning one validated configured country for all inputs.
     *
     * @param countryCode country configured only for local/test execution
     */
    public StubGeoLocationResolver(CountryCode countryCode) {
        this.countryCode = Objects.requireNonNull(countryCode);
    }

    /** {@inheritDoc} */
    @Override
    public CountryCode resolve(ClientIpAddress clientIpAddress) {
        Objects.requireNonNull(clientIpAddress);
        return countryCode;
    }
}
