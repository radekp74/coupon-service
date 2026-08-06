package pl.radoslawpiatek.couponservice.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UserIdTest {
    @Test void preservesValidOpaqueVisibleAsciiWithoutNormalization() {
        for (String value : new String[]{"!", "a".repeat(128), "auth0|123", "tenant/user+external", "customer@example.com", "urn:customer:123"}) {
            assertThat(UserId.of(value).value()).isEqualTo(value);
        }
        assertThat(UserId.of("customer-A")).isNotEqualTo(UserId.of("customer-a"));
    }

    @Test void rejectsEveryValueOutsideVisibleAsciiContract() {
        for (String value : new String[]{"", "a".repeat(129), " leading", "trailing ", " ", "\t", "\n", "a\u0001b", "a\u007fb", "żółć"}) {
            assertThatThrownBy(() -> UserId.of(value)).isInstanceOf(InvalidCouponValueException.class);
        }
        assertThatThrownBy(() -> UserId.of(null)).isInstanceOf(InvalidCouponValueException.class);
    }
}
