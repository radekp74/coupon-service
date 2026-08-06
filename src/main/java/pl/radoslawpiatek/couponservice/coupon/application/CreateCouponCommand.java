package pl.radoslawpiatek.couponservice.coupon.application;

public record CreateCouponCommand(String code, int maxUses, String countryCode) {
}
