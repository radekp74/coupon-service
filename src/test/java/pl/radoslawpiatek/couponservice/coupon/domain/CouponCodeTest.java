package pl.radoslawpiatek.couponservice.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CouponCodeTest {

    @Test
    void trimsThePresentationValueAndBuildsALocaleIndependentCanonicalValue() {
        CouponCode code = CouponCode.of("  WiOsNa-2026  ");

        assertThat(code.value()).isEqualTo("WiOsNa-2026");
        assertThat(code.normalizedValue()).isEqualTo("WIOSNA-2026");
    }

    @Test
    void comparesCodesByCanonicalValue() {
        assertThat(CouponCode.of("WIOSNA")).isEqualTo(CouponCode.of("wiosna"));
    }

    @Test
    void rejectsUnsupportedCharacters() {
        assertThatThrownBy(() -> CouponCode.of("wiosna 2026"))
                .isInstanceOf(InvalidCouponValueException.class)
                .hasMessageContaining("ASCII letters");
    }
}
