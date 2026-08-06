package pl.radoslawpiatek.couponservice.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CountryCodeTest {

    @Test
    void normalizesAnIsoCountryCode() {
        assertThat(CountryCode.of(" pl ").value()).isEqualTo("PL");
    }

    @Test
    void rejectsUnknownCountryCode() {
        assertThatThrownBy(() -> CountryCode.of("XX"))
                .isInstanceOf(InvalidCouponValueException.class)
                .hasMessageContaining("ISO 3166-1");
    }
}
