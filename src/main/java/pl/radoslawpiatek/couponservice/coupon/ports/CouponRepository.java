package pl.radoslawpiatek.couponservice.coupon.ports;

import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;

public interface CouponRepository {

    void insert(Coupon coupon);
}
