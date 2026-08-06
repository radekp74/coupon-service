package pl.radoslawpiatek.couponservice.coupon.domain;

/**
 * Signals that a public coupon value does not satisfy the frozen domain contract.
 */
public final class InvalidCouponValueException extends RuntimeException {

    private final String field;

    public InvalidCouponValueException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
