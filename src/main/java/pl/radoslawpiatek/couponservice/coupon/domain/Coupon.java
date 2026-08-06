package pl.radoslawpiatek.couponservice.coupon.domain;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/** Immutable representation of a coupon at creation time. */
public record Coupon(
        UUID id,
        CouponCode code,
        OffsetDateTime createdAt,
        int maxUses,
        int currentUses,
        CountryCode countryCode
) {
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
