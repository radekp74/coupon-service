package pl.radoslawpiatek.couponservice.coupon.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Opaque, case-sensitive identity supplied by a redemption client.
 *
 * <p>The value is deliberately neither trimmed nor normalized. It is restricted to one through 128
 * visible ASCII characters so the HTTP contract, PostgreSQL constraint and application validation
 * can have exactly the same meaning.
 *
 * @param value exact visible-ASCII identity retained without trimming or canonicalization
 */
public record UserId(String value) {

    private static final Pattern VISIBLE_ASCII = Pattern.compile("^[!-~]{1,128}$");

    /**
     * Validates one unchanged client-supplied identifier.
     *
     * @param rawValue opaque value exactly as received from the HTTP payload
     * @return validated value without any canonicalization
     * @throws InvalidCouponValueException when the value is absent or outside the visible ASCII contract
     */
    public static UserId of(String rawValue) {
        if (rawValue == null || !VISIBLE_ASCII.matcher(rawValue).matches()) {
            throw new InvalidCouponValueException("userId", "User ID must contain 1 to 128 visible ASCII characters.");
        }
        return new UserId(rawValue);
    }

    /** Enforces the invariant even for direct construction inside the application. */
    public UserId {
        Objects.requireNonNull(value, "value");
        if (!VISIBLE_ASCII.matcher(value).matches()) {
            throw new InvalidCouponValueException("userId", "User ID must contain 1 to 128 visible ASCII characters.");
        }
    }
}
