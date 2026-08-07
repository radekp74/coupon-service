package pl.radoslawpiatek.couponservice.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;
import pl.radoslawpiatek.couponservice.coupon.ports.CouponRepository;
import pl.radoslawpiatek.couponservice.coupon.ports.UuidGenerator;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics;

@ExtendWith(MockitoExtension.class)
class CreateCouponServiceTest {

    private static final UUID COUPON_ID = UUID.fromString("149b508d-3797-466a-859f-5fb0770dcb0d");
    private static final Instant CREATED_AT = Instant.parse("2026-08-06T12:30:00Z");

    @Mock
    private CouponRepository couponRepository;

    @Test
    void createsAndPersistsACouponWithInjectedIdentityAndTime() {
        UuidGenerator uuidGenerator = () -> COUPON_ID;
        Clock clock = Clock.fixed(CREATED_AT, ZoneOffset.UTC);
        CreateCouponService service = new CreateCouponService(couponRepository, uuidGenerator, clock, new CouponServiceMetrics(new SimpleMeterRegistry()));

        Coupon created = service.create(new CreateCouponCommand(" wiosna ", 100, "pl"));

        assertThat(created.id()).isEqualTo(COUPON_ID);
        assertThat(created.code().value()).isEqualTo("wiosna");
        assertThat(created.code().normalizedValue()).isEqualTo("WIOSNA");
        assertThat(created.createdAt().toInstant()).isEqualTo(CREATED_AT);
        assertThat(created.maxUses()).isEqualTo(100);
        assertThat(created.currentUses()).isZero();
        assertThat(created.countryCode().value()).isEqualTo("PL");

        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(couponRepository).insert(captor.capture());
        assertThat(captor.getValue()).isEqualTo(created);
    }
}
