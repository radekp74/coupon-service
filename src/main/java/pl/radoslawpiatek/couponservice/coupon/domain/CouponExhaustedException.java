package pl.radoslawpiatek.couponservice.coupon.domain;

/** Signals that no committed use remains for a coupon. */
public final class CouponExhaustedException extends RuntimeException {
    /** Creates the stable quota failure. */
    public CouponExhaustedException() { super("Coupon usage limit reached."); }
}
