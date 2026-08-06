package pl.radoslawpiatek.couponservice.coupon.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponCode;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.coupon.ports.CouponRepository;
import pl.radoslawpiatek.couponservice.coupon.ports.UuidGenerator;

@Service
public class CreateCouponService implements CreateCouponUseCase {

    private final CouponRepository couponRepository;
    private final UuidGenerator uuidGenerator;
    private final Clock clock;

    public CreateCouponService(
            CouponRepository couponRepository,
            UuidGenerator uuidGenerator,
            Clock clock
    ) {
        this.couponRepository = Objects.requireNonNull(couponRepository);
        this.uuidGenerator = Objects.requireNonNull(uuidGenerator);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional
    public Coupon create(CreateCouponCommand command) {
        Objects.requireNonNull(command, "command");

        Coupon coupon = Coupon.create(
                uuidGenerator.next(),
                CouponCode.of(command.code()),
                OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
                command.maxUses(),
                CountryCode.of(command.countryCode())
        );

        couponRepository.insert(coupon);
        return coupon;
    }
}
