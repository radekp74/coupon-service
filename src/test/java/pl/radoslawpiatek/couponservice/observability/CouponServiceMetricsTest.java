package pl.radoslawpiatek.couponservice.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Set;
import org.junit.jupiter.api.Test;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics.ClientIpOutcome;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics.ClientIpSource;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics.CreateOutcome;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics.GeoProvider;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics.GeolocationOutcome;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics.RedemptionOutcome;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics.TransactionOutcome;

class CouponServiceMetricsTest {

    @Test
    void registersSixFrozenMeterFamiliesAndOnlyLowCardinalityTagKeys() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CouponServiceMetrics metrics = new CouponServiceMetrics(registry);

        assertThat(registry.getMeters().stream().map(meter -> meter.getId().getName()).collect(java.util.stream.Collectors.toSet()))
                .contains("coupon.create", "coupon.redemption", "client.ip.resolution",
                        "geolocation.resolution", "geolocation.provider", "coupon.redemption.transaction");

        Set<String> allowedKeys = Set.of("outcome", "source", "provider");
        for (Meter meter : registry.getMeters()) {
            assertThat(meter.getId().getTags()).allSatisfy(tag -> assertThat(allowedKeys).contains(tag.getKey()));
        }

        metrics.recordCreate(CreateOutcome.SUCCESS);
        metrics.recordRedemption(RedemptionOutcome.EXHAUSTED);
        metrics.recordClientIp(ClientIpSource.FORWARDED, ClientIpOutcome.FAILURE);
        metrics.recordGeolocation(GeoProvider.IPWHOIS, GeolocationOutcome.TIMEOUT);
        Timer.Sample geo = metrics.startGeolocationTimer();
        metrics.stopGeolocationTimer(geo, GeoProvider.IPWHOIS, GeolocationOutcome.TIMEOUT);
        Timer.Sample tx = metrics.startTransactionTimer();
        metrics.stopTransactionTimer(tx, TransactionOutcome.SUCCESS);

        assertThat(registry.get("coupon.create").tag("outcome", "success").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("coupon.redemption").tag("outcome", "exhausted").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("client.ip.resolution").tags("source", "forwarded", "outcome", "failure").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("geolocation.resolution").tags("provider", "ipwhois", "outcome", "timeout").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("geolocation.provider").tags("provider", "ipwhois", "outcome", "timeout").timer().count()).isEqualTo(1L);
        assertThat(registry.get("coupon.redemption.transaction").tag("outcome", "success").timer().count()).isEqualTo(1L);
    }
}
