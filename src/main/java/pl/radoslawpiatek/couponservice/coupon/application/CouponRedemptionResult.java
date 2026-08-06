package pl.radoslawpiatek.couponservice.coupon.application;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Committed redemption state returned after its insert and counter increment share one transaction.
 *
 * @param redemptionId generated persistent identity
 * @param couponCode presentation code from the locked coupon
 * @param userId exact opaque client identifier
 * @param redeemedAt server-generated commit-time value
 * @param remainingUses counter-derived capacity after the increment
 */
public record CouponRedemptionResult(UUID redemptionId, String couponCode, String userId,
                                    OffsetDateTime redeemedAt, int remainingUses) { }
