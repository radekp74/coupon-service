package pl.radoslawpiatek.couponservice.coupon.adapters.web;

import java.time.OffsetDateTime;
import java.util.UUID;
import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;

public record CouponResponse(
        UUID id,
        String code,
        OffsetDateTime createdAt,
        int maxUses,
        int currentUses,
        String countryCode
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.id(),
                coupon.code().value(),
                coupon.createdAt(),
                coupon.maxUses(),
                coupon.currentUses(),
                coupon.countryCode().value()
        );
    }
}
