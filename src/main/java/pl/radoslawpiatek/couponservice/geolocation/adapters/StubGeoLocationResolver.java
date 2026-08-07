package pl.radoslawpiatek.couponservice.geolocation.adapters;

import io.micrometer.core.instrument.Timer;
import java.util.Objects;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;
import pl.radoslawpiatek.couponservice.geolocation.ports.GeoLocationResolver;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics.GeoProvider;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics.GeolocationOutcome;

/**
 * Deterministic local/test resolver that never sends a client address across the network.
 *
 * <p>Construction is restricted by {@code GeolocationConfiguration}; this class itself contains no
 * profile switch so callers cannot turn a request header into a country-selection bypass.
 */
public final class StubGeoLocationResolver implements GeoLocationResolver {

    private final CountryCode countryCode;
    private final CouponServiceMetrics metrics;

    /**
     * Creates a resolver returning one validated configured country for all inputs.
     *
     * @param countryCode country configured only for local/test execution
     * @param metrics low-cardinality provider metrics
     */
    public StubGeoLocationResolver(CountryCode countryCode, CouponServiceMetrics metrics) {
        this.countryCode = Objects.requireNonNull(countryCode);
        this.metrics = Objects.requireNonNull(metrics);
    }

    /** {@inheritDoc} */
    @Override
    public CountryCode resolve(ClientIpAddress clientIpAddress) {
        Objects.requireNonNull(clientIpAddress);
        Timer.Sample sample = metrics.startGeolocationTimer();
        try {
            metrics.recordGeolocation(GeoProvider.STUB, GeolocationOutcome.SUCCESS);
            return countryCode;
        } finally {
            metrics.stopGeolocationTimer(sample, GeoProvider.STUB, GeolocationOutcome.SUCCESS);
        }
    }
}
