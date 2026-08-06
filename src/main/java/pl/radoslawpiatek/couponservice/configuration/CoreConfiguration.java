package pl.radoslawpiatek.couponservice.configuration;

import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.radoslawpiatek.couponservice.coupon.ports.UuidGenerator;

/** Provides deterministic application ports for time and identifier generation. */
@Configuration
public class CoreConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    UuidGenerator uuidGenerator() {
        return UUID::randomUUID;
    }
}
