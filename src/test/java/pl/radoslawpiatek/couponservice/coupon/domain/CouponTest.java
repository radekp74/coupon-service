package pl.radoslawpiatek.couponservice.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CouponTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.of(2026, 8, 7, 6, 0, 0, 0, ZoneOffset.UTC);
    private static final CouponCode CODE = CouponCode.of("QUALITY");
    private static final CountryCode COUNTRY = CountryCode.of("PL");

    @Test
    void createStartsWithZeroCommittedUses() {
        Coupon coupon = Coupon.create(ID, CODE, CREATED_AT, 10, COUNTRY);

        assertThat(coupon.currentUses()).isZero();
        assertThat(coupon.maxUses()).isEqualTo(10);
    }

    @Test
    void enforcesPublicMaximumUseRange() {
        assertThat(new Coupon(ID, CODE, CREATED_AT, 1, 0, COUNTRY).maxUses()).isEqualTo(1);
        assertThat(new Coupon(ID, CODE, CREATED_AT, 1_000_000, 0, COUNTRY).maxUses()).isEqualTo(1_000_000);
        assertThatThrownBy(() -> new Coupon(ID, CODE, CREATED_AT, 0, 0, COUNTRY))
                .isInstanceOf(InvalidCouponValueException.class);
        assertThatThrownBy(() -> new Coupon(ID, CODE, CREATED_AT, 1_000_001, 0, COUNTRY))
                .isInstanceOf(InvalidCouponValueException.class);
    }

    @Test
    void currentUsesMustStayBetweenZeroAndMaximum() {
        assertThat(new Coupon(ID, CODE, CREATED_AT, 10, 10, COUNTRY).currentUses()).isEqualTo(10);
        assertThatThrownBy(() -> new Coupon(ID, CODE, CREATED_AT, 10, -1, COUNTRY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Coupon(ID, CODE, CREATED_AT, 10, 11, COUNTRY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiredStateCannotBeNull() {
        assertThatThrownBy(() -> new Coupon(null, CODE, CREATED_AT, 10, 0, COUNTRY)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Coupon(ID, null, CREATED_AT, 10, 0, COUNTRY)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Coupon(ID, CODE, null, 10, 0, COUNTRY)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Coupon(ID, CODE, CREATED_AT, 10, 0, null)).isInstanceOf(NullPointerException.class);
    }
}
