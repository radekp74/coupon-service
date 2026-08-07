package pl.radoslawpiatek.couponservice.coupon.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponCode;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponCodeConflictException;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.coupon.ports.CouponRepository;
import pl.radoslawpiatek.couponservice.coupon.ports.UuidGenerator;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics.CreateOutcome;

/**
 * Transactional implementation of coupon creation.
 *
 * <p>The service deliberately performs no preflight existence check. PostgreSQL
 * remains the concurrency-safe authority for canonical code uniqueness.
 */
@Service
public class CreateCouponService implements CreateCouponUseCase {

    private final CouponRepository couponRepository;
    private final UuidGenerator uuidGenerator;
    private final Clock clock;
    private final CouponServiceMetrics metrics;

    /**
     * Creates the use-case implementation with infrastructure ports supplied by Spring.
     *
     * @param couponRepository persistence authority for uniqueness conflicts
     * @param uuidGenerator source of identifiers for new coupons
     * @param clock source of the persisted UTC creation time
     * @param metrics low-cardinality business-outcome metrics
     */
    public CreateCouponService(
            CouponRepository couponRepository,
            UuidGenerator uuidGenerator,
            Clock clock,
            CouponServiceMetrics metrics
    ) {
        this.couponRepository = Objects.requireNonNull(couponRepository);
        this.uuidGenerator = Objects.requireNonNull(uuidGenerator);
        this.clock = Objects.requireNonNull(clock);
        this.metrics = Objects.requireNonNull(metrics);
    }

    /** {@inheritDoc} */
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

        try {
            couponRepository.insert(coupon);
            metrics.recordCreate(CreateOutcome.SUCCESS);
            return coupon;
        } catch (CouponCodeConflictException exception) {
            metrics.recordCreate(CreateOutcome.CONFLICT);
            throw exception;
        }
    }
}
