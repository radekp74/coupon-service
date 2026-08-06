package pl.radoslawpiatek.couponservice.coupon.domain;

/** Raised when a canonical coupon code already exists. */
public final class CouponCodeConflictException extends RuntimeException {

    private final String normalizedCode;

    public CouponCodeConflictException(String normalizedCode, Throwable cause) {
        super("Coupon code already exists.", cause);
        this.normalizedCode = normalizedCode;
    }

    public String normalizedCode() {
        return normalizedCode;
    }
}
