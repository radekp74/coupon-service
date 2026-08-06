package pl.radoslawpiatek.couponservice.coupon.domain;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable coupon state persisted by the service.
 *
 * @param id server-generated identifier
 * @param code validated presentation and canonical coupon code
 * @param createdAt server-generated creation time in UTC
 * @param maxUses maximum number of successful redemptions
 * @param currentUses number of committed redemptions
 * @param countryCode country in which the coupon may be redeemed
 */
public record Coupon(
        UUID id,
        CouponCode code,
        OffsetDateTime createdAt,
        int maxUses,
        int currentUses,
        CountryCode countryCode
) {
    /**
     * Enforces the state invariants that must also hold for persisted coupons.
     *
     * @throws InvalidCouponValueException when the configured redemption limit is outside its public range
     * @throws IllegalArgumentException when the current use count is not within zero and the limit
     */
    public Coupon {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(countryCode, "countryCode");
        if (maxUses < 1 || maxUses > 1_000_000) {
            throw new InvalidCouponValueException("maxUses", "Maximum uses must be between 1 and 1000000.");
        }
        if (currentUses < 0 || currentUses > maxUses) {
            throw new IllegalArgumentException("currentUses must remain between zero and maxUses");
        }
    }

    /**
     * Creates the initial coupon state with zero recorded uses.
     *
     * @param id server-generated identifier
     * @param code validated coupon code
     * @param createdAt server-generated creation time
     * @param maxUses configured redemption limit
     * @param countryCode allowed country
     * @return a coupon whose current use count is zero
     */
    public static Coupon create(
            UUID id,
            CouponCode code,
            OffsetDateTime createdAt,
            int maxUses,
            CountryCode countryCode
    ) {
        return new Coupon(id, code, createdAt, maxUses, 0, countryCode);
    }
}
