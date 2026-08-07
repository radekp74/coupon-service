package pl.radoslawpiatek.couponservice.coupon.application;

/**
 * HTTP-independent input for one redemption attempt.
 *
 * @param code externally supplied coupon code validated by the application layer
 * @param userId exact opaque user identity; it is neither trimmed nor normalized
 */
public record RedeemCouponCommand(String code, String userId) { }
