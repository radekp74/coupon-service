package pl.radoslawpiatek.couponservice.coupon.domain;

/** Signals that PostgreSQL rejected a case-insensitive duplicate coupon code. */
public final class CouponCodeConflictException extends RuntimeException {

    private final String normalizedCode;

    /**
     * Creates a conflict without exposing the underlying database details to API clients.
     *
     * @param normalizedCode canonical code that violated uniqueness
     * @param cause original persistence failure retained for technical diagnostics
     */
    public CouponCodeConflictException(String normalizedCode, Throwable cause) {
        super("Coupon code already exists.", cause);
        this.normalizedCode = normalizedCode;
    }

    /**
     * Returns the code used to classify the failed uniqueness attempt.
     *
     * @return the canonical code that caused the conflict
     */
    public String normalizedCode() {
        return normalizedCode;
    }
}
