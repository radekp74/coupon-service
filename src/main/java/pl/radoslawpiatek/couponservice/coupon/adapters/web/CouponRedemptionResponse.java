package pl.radoslawpiatek.couponservice.coupon.adapters.web;

import java.time.OffsetDateTime;
import java.util.UUID;
import pl.radoslawpiatek.couponservice.coupon.application.CouponRedemptionResult;

/** Public representation of one committed coupon redemption. */
public record CouponRedemptionResponse(UUID redemptionId, String couponCode, String userId,
                                       OffsetDateTime redeemedAt, int remainingUses) {
    /** @param result committed application result @return HTTP-safe response without IP or provider data */
    public static CouponRedemptionResponse from(CouponRedemptionResult result) {
        return new CouponRedemptionResponse(result.redemptionId(), result.couponCode(), result.userId(),
                result.redeemedAt(), result.remainingUses());
    }
}
