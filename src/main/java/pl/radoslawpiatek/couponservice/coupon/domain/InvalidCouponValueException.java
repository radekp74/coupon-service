package pl.radoslawpiatek.couponservice.coupon.domain;

/** Signals that an externally supplied coupon value violates a domain invariant. */
public final class InvalidCouponValueException extends RuntimeException {

    private final String field;

    /**
     * Creates a validation failure for one public field.
     *
     * @param field stable field name used for diagnostics
     * @param message safe human-readable explanation
     */
    public InvalidCouponValueException(String field, String message) {
        super(message);
        this.field = field;
    }

    /**
     * Returns the stable public field name for a validation failure.
     *
     * @return the public field whose value was rejected
     */
    public String field() {
        return field;
    }
}
