package pl.radoslawpiatek.couponservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starts the coupon service Spring Boot application. */
@SpringBootApplication
public class CouponServiceApplication {

    /**
     * Starts the application using the externalized Spring configuration.
     *
     * @param args command-line arguments forwarded to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(CouponServiceApplication.class, args);
    }
}
