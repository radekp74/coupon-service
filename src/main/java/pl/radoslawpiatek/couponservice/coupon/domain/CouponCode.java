package pl.radoslawpiatek.couponservice.coupon.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Case-insensitive coupon code with separate presentation and canonical values.
 *
 * <p>The presentation value is trimmed and returned to API clients. Equality and
 * database lookup use the uppercase canonical value produced with {@link Locale#ROOT}.
 */
public final class CouponCode {

    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9_-]{3,64}");

    private final String value;
    private final String normalizedValue;

    private CouponCode(String value, String normalizedValue) {
        this.value = value;
        this.normalizedValue = normalizedValue;
    }

    /**
     * Validates and canonicalizes an externally supplied coupon code.
     *
     * @param rawValue code supplied by an API client
     * @return a validated code containing presentation and canonical values
     * @throws InvalidCouponValueException when the value is null or violates the frozen format
     */
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

    /**
     * Returns the presentation value retained for API responses.
     *
     * @return the trimmed presentation value returned to API clients
     */
    public String value() {
        return value;
    }

    /**
     * Returns the database uniqueness key, not the client-facing presentation value.
     *
     * @return the uppercase value used for equality, lookup and uniqueness
     */
    public String normalizedValue() {
        return normalizedValue;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof CouponCode couponCode
                && normalizedValue.equals(couponCode.normalizedValue);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(normalizedValue);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return value;
    }
}
