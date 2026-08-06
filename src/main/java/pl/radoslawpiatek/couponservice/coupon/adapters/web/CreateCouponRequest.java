package pl.radoslawpiatek.couponservice.coupon.adapters.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCouponRequest(
        @NotBlank String code,
        @NotNull @Min(1) @Max(1_000_000) Integer maxUses,
        @NotBlank String countryCode
) {
}
