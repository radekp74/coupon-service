package pl.radoslawpiatek.couponservice.coupon.application;

import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.coupon.domain.UserId;

/**
 * Transactional boundary that accepts only canonical domain values and never raw request or IP data.
 *
 * <p>The implementation owns the PostgreSQL row lock, duplicate-user check, capacity check,
 * redemption insert and conditional counter increment as one atomic commit.
 */
public interface CouponRedemptionTransaction {

    /**
     * Commits one redemption after revalidating the locked coupon state.
     *
     * @param normalizedCode canonical coupon lookup key established before the transaction
     * @param userId exact opaque user identity that must redeem at most once
     * @param resolvedCountry country resolved outside the transaction and rechecked under the lock
     * @return committed redemption state after insert and counter increment
     */
    CouponRedemptionResult redeem(String normalizedCode, UserId userId, CountryCode resolvedCountry);
}
