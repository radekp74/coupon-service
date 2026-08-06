package pl.radoslawpiatek.couponservice.coupon.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponExhaustedException;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponNotFoundException;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryNotAllowedException;
import pl.radoslawpiatek.couponservice.coupon.domain.UserId;
import pl.radoslawpiatek.couponservice.coupon.ports.CouponRedemptionRepository;
import pl.radoslawpiatek.couponservice.coupon.ports.UuidGenerator;

/**
 * Short proxied PostgreSQL transaction for redemption consistency.
 *
 * <p>Country resolution occurs before this bean is invoked, so no HTTP or raw IP can extend the
 * row lock. The lock order deliberately gives country, then duplicate user, then exhaustion.
 */
@Service
public class TransactionalCouponRedemptionService implements CouponRedemptionTransaction {
    private final CouponRedemptionRepository repository;
    private final UuidGenerator uuidGenerator;
    private final Clock clock;

    /** @param repository database operations guarded by this transaction @param uuidGenerator ID source @param clock UTC time source */
    public TransactionalCouponRedemptionService(CouponRedemptionRepository repository, UuidGenerator uuidGenerator, Clock clock) {
        this.repository = Objects.requireNonNull(repository); this.uuidGenerator = Objects.requireNonNull(uuidGenerator); this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CouponRedemptionResult redeem(String normalizedCode, UserId userId, CountryCode resolvedCountry) {
        Coupon coupon = repository.findForUpdate(normalizedCode).orElseThrow(CouponNotFoundException::new);
        if (!coupon.countryCode().equals(resolvedCountry)) throw new CountryNotAllowedException();
        if (repository.exists(coupon.id(), userId)) throw new pl.radoslawpiatek.couponservice.coupon.domain.CouponAlreadyRedeemedException();
        if (coupon.currentUses() >= coupon.maxUses()) throw new CouponExhaustedException();
        var id = uuidGenerator.next(); var redeemedAt = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        repository.insert(id, coupon.id(), userId, resolvedCountry.value(), redeemedAt);
        int currentUses = repository.incrementIfCapacity(coupon.id()).orElseThrow(IllegalStateException::new);
        return new CouponRedemptionResult(id, coupon.code().value(), userId.value(), redeemedAt, coupon.maxUses() - currentUses);
    }
}
