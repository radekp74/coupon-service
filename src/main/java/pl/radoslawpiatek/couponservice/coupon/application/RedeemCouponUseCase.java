package pl.radoslawpiatek.couponservice.coupon.application;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Redeems a coupon after resolving server-side request context.
 *
 * <p>The servlet request is accepted only to resolve the verified Client IP before the transaction;
 * raw IP data is never passed to the transactional persistence boundary.
 */
public interface RedeemCouponUseCase {
    /**
     * Executes the ordered snapshot, GeoIP and transactional redemption flow.
     *
     * @param command code and opaque user identifier
     * @param request transport context inspected outside the database transaction
     * @return committed redemption response state
     */
    CouponRedemptionResult redeem(RedeemCouponCommand command, HttpServletRequest request);
}
