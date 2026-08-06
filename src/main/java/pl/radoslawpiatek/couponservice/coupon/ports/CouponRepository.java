package pl.radoslawpiatek.couponservice.coupon.ports;

import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;

/** Persistence port for coupon state. */
public interface CouponRepository {

    /**
     * Persists one coupon and lets the database resolve canonical uniqueness races.
     *
     * @param coupon validated coupon state
     */
    void insert(Coupon coupon);
}
