package pl.radoslawpiatek.couponservice.coupon.ports;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;
import pl.radoslawpiatek.couponservice.coupon.domain.UserId;

/** Persistence operations used exclusively by the redemption flow. */
public interface CouponRedemptionRepository {
    /** @param normalizedCode canonical coupon code @return snapshot without a row lock */
    Optional<Coupon> findSnapshot(String normalizedCode);

    /** @param normalizedCode canonical coupon code @return locked coupon row or empty when deleted meanwhile */
    Optional<Coupon> findForUpdate(String normalizedCode);

    /** @return whether the user already has a committed redemption for the coupon */
    boolean exists(UUID couponId, UserId userId);

    /** Inserts a redemption record; only the named user/coupon unique constraint becomes a domain conflict. */
    void insert(UUID redemptionId, UUID couponId, UserId userId, String countryCode, OffsetDateTime redeemedAt);

    /** @return updated current-use count, or empty if the conditional counter update affected no row */
    Optional<Integer> incrementIfCapacity(UUID couponId);
}
