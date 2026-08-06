package pl.radoslawpiatek.couponservice.coupon.application;

/**
 * Application command for creating a coupon.
 *
 * @param code externally supplied presentation code
 * @param maxUses requested redemption limit
 * @param countryCode externally supplied ISO country code
 */
public record CreateCouponCommand(String code, int maxUses, String countryCode) {
}
