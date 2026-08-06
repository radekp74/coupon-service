package pl.radoslawpiatek.couponservice.coupon.ports;

import java.util.UUID;

@FunctionalInterface
public interface UuidGenerator {

    UUID next();
}
