package pl.radoslawpiatek.couponservice.coupon.adapters.web;

import java.time.OffsetDateTime;
import java.util.UUID;
import pl.radoslawpiatek.couponservice.coupon.application.CouponRedemptionResult;

/**
 * Public representation of one committed coupon redemption.
 *
 * <p>The response intentionally contains no client IP, resolved country or provider diagnostics.
 *
 * @param redemptionId persistent server-generated redemption identifier
 * @param couponCode client-facing coupon presentation code
 * @param userId exact opaque identifier that was committed
 * @param redeemedAt server-generated redemption time
 * @param remainingUses remaining capacity derived from the committed counter
 */
public record CouponRedemptionResponse(UUID redemptionId, String couponCode, String userId,
                                       OffsetDateTime redeemedAt, int remainingUses) {
    /**
     * Maps the committed application result to the privacy-safe HTTP representation.
     *
     * @param result committed application result
     * @return response containing only the public redemption contract
     */
    public static CouponRedemptionResponse from(CouponRedemptionResult result) {
        return new CouponRedemptionResponse(result.redemptionId(), result.couponCode(), result.userId(),
                result.redeemedAt(), result.remainingUses());
    }
}
