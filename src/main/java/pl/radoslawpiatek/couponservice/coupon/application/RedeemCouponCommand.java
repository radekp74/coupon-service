package pl.radoslawpiatek.couponservice.coupon.application;

/** HTTP-independent input for a redemption attempt. */
public record RedeemCouponCommand(String code, String userId) { }
