package pl.radoslawpiatek.couponservice.coupon.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Case-insensitive coupon code with an explicitly stored presentation value and canonical value.
 */
public final class CouponCode {

    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9_-]{3,64}");

    private final String value;
    private final String normalizedValue;

    private CouponCode(String value, String normalizedValue) {
        this.value = value;
        this.normalizedValue = normalizedValue;
    }

    public static CouponCode of(String rawValue) {
        if (rawValue == null) {
            throw new InvalidCouponValueException("code", "Coupon code is required.");
        }

        String trimmed = rawValue.trim();
        if (!FORMAT.matcher(trimmed).matches()) {
            throw new InvalidCouponValueException(
                    "code",
                    "Coupon code must contain 3 to 64 ASCII letters, digits, underscores or hyphens."
            );
        }

        return new CouponCode(trimmed, trimmed.toUpperCase(Locale.ROOT));
    }

    public String value() {
        return value;
    }

    public String normalizedValue() {
        return normalizedValue;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof CouponCode couponCode
                && normalizedValue.equals(couponCode.normalizedValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalizedValue);
    }

    @Override
    public String toString() {
        return value;
    }
}
