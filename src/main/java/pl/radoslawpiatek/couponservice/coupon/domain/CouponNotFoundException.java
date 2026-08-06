package pl.radoslawpiatek.couponservice.coupon.domain;

/** Signals that a canonical coupon code has no persisted state. */
public final class CouponNotFoundException extends RuntimeException {
    /** Creates a privacy-safe absence failure without echoing request data. */
    public CouponNotFoundException() { super("Coupon was not found."); }
}
