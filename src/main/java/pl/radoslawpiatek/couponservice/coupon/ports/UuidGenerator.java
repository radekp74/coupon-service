package pl.radoslawpiatek.couponservice.coupon.ports;

import java.util.UUID;

/** Generates identifiers without coupling application logic to a static UUID call. */
@FunctionalInterface
public interface UuidGenerator {

    /**
     * Produces one identifier for a new aggregate.
     *
     * @return the next server-generated identifier
     */
    UUID next();
}
