package pl.radoslawpiatek.couponservice.coupon.ports;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;
import pl.radoslawpiatek.couponservice.coupon.domain.UserId;

/**
 * Persistence operations used exclusively by the redemption flow.
 *
 * <p>The port separates the non-locking snapshot read from the row-locking transactional read.
 * Implementations must keep redemption insertion and the capacity increment inside the caller's
 * transaction and may translate only the named coupon/user uniqueness constraint into the domain
 * duplicate-redemption error.
 */
public interface CouponRedemptionRepository {

    /**
     * Reads the coupon without acquiring a row lock so GeoIP can run outside the transaction.
     *
     * @param normalizedCode canonical case-insensitive coupon lookup key
     * @return current coupon snapshot, or empty when the code does not exist
     */
    Optional<Coupon> findSnapshot(String normalizedCode);

    /**
     * Reads and locks the coupon row for the short redemption transaction.
     *
     * @param normalizedCode canonical case-insensitive coupon lookup key
     * @return locked coupon state, or empty if the coupon disappeared after the snapshot read
     */
    Optional<Coupon> findForUpdate(String normalizedCode);

    /**
     * Checks the committed one-redemption-per-user invariant while the coupon row is locked.
     *
     * @param couponId persistent coupon identifier
     * @param userId exact case-sensitive user identifier
     * @return {@code true} when a committed redemption already exists for the pair
     */
    boolean exists(UUID couponId, UserId userId);

    /**
     * Persists one redemption in the transaction that also increments the coupon counter.
     *
     * @param redemptionId server-generated redemption identifier
     * @param couponId locked coupon identifier
     * @param userId exact case-sensitive user identifier
     * @param countryCode resolved country that passed the locked country check
     * @param redeemedAt server-generated redemption time
     * @throws pl.radoslawpiatek.couponservice.coupon.domain.CouponAlreadyRedeemedException
     *         when the named coupon/user unique constraint loses a race
     */
    void insert(UUID redemptionId, UUID couponId, UserId userId, String countryCode, OffsetDateTime redeemedAt);

    /**
     * Atomically increments the committed-use counter only while capacity remains.
     *
     * @param couponId locked coupon identifier
     * @return updated current-use count, or empty when the conditional update affected no row
     */
    Optional<Integer> incrementIfCapacity(UUID couponId);
}
