package pl.radoslawpiatek.couponservice.coupon.application;

import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;

public interface CreateCouponUseCase {

    Coupon create(CreateCouponCommand command);
}
