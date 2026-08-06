package pl.radoslawpiatek.couponservice.coupon.adapters.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * JSON payload accepted by the coupon creation endpoint.
 *
 * @param code presentation code; domain validation applies after Bean Validation
 * @param maxUses maximum number of successful redemptions
 * @param countryCode ISO 3166-1 alpha-2 country code
 */
public record CreateCouponRequest(
        @NotBlank String code,
        @NotNull @Min(1) @Max(1_000_000) Integer maxUses,
        @NotBlank String countryCode
) {
}
