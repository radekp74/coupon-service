package pl.radoslawpiatek.couponservice.coupon.adapters.web;

import java.time.OffsetDateTime;
import java.util.UUID;
import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;

/**
 * Public representation of committed coupon state.
 *
 * @param id server-generated identifier
 * @param code trimmed presentation code
 * @param createdAt creation time in UTC
 * @param maxUses configured redemption limit
 * @param currentUses committed redemption count
 * @param countryCode normalized allowed country
 */
public record CouponResponse(
        UUID id,
        String code,
        OffsetDateTime createdAt,
        int maxUses,
        int currentUses,
        String countryCode
) {
    /**
     * Maps the domain state without exposing canonical or persistence-only fields.
     *
     * @param coupon committed domain state
     * @return API response
     */
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
