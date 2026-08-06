package pl.radoslawpiatek.couponservice.coupon.domain;

/** Signals that the same opaque user has already committed a redemption for the coupon. */
public final class CouponAlreadyRedeemedException extends RuntimeException {
    /** Creates the stable retry outcome without disclosing persistence details. */
    public CouponAlreadyRedeemedException() { super("Coupon was already redeemed."); }
}
