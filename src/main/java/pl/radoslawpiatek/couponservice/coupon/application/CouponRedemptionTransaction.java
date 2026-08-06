package pl.radoslawpiatek.couponservice.coupon.application;

import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.coupon.domain.UserId;

/** Transactional boundary that accepts only canonical domain values and never raw request/IP data. */
public interface CouponRedemptionTransaction {
    /** @return committed redemption result after lock, insert and increment */
    CouponRedemptionResult redeem(String normalizedCode, UserId userId, CountryCode resolvedCountry);
}
