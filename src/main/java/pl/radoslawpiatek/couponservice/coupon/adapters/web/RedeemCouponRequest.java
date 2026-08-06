package pl.radoslawpiatek.couponservice.coupon.adapters.web;

import jakarta.validation.constraints.Pattern;

/**
 * Public redemption payload containing only a client-declared opaque user identity.
 *
 * @param userId exact case-sensitive visible ASCII identifier; it is not trimmed or normalized
 */
public record RedeemCouponRequest(
        @Pattern(regexp = "^[!-~]{1,128}$") String userId
) { }
