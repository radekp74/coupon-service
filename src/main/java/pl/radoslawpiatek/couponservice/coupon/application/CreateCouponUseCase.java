package pl.radoslawpiatek.couponservice.coupon.application;

import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;

/** Public application contract for creating one case-insensitively unique coupon. */
public interface CreateCouponUseCase {

    /**
     * Validates and persists one coupon.
     *
     * @param command untrusted creation data from the API boundary
     * @return the committed coupon state
     * @throws pl.radoslawpiatek.couponservice.coupon.domain.CouponCodeConflictException
     *         when the canonical code already exists
     * @throws pl.radoslawpiatek.couponservice.coupon.domain.InvalidCouponValueException
     *         when a domain value is invalid
     */
    Coupon create(CreateCouponCommand command);
}
