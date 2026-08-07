package pl.radoslawpiatek.couponservice.coupon.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import io.micrometer.core.instrument.Timer;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponAlreadyRedeemedException;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponExhaustedException;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponNotFoundException;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryNotAllowedException;
import pl.radoslawpiatek.couponservice.coupon.domain.UserId;
import pl.radoslawpiatek.couponservice.coupon.ports.CouponRedemptionRepository;
import pl.radoslawpiatek.couponservice.coupon.ports.UuidGenerator;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics.TransactionOutcome;

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
    private final CouponServiceMetrics metrics;

    /**
     * Creates the short transaction service without any HTTP or geolocation dependency.
     *
     * @param repository database operations executed under the same transaction and row lock
     * @param uuidGenerator source of persistent redemption identifiers
     * @param clock server-side time source used for the committed redemption timestamp
     * @param metrics timer facade whose scope is restricted to this database transaction
     */
    public TransactionalCouponRedemptionService(CouponRedemptionRepository repository, UuidGenerator uuidGenerator, Clock clock, CouponServiceMetrics metrics) {
        this.repository = Objects.requireNonNull(repository); this.uuidGenerator = Objects.requireNonNull(uuidGenerator); this.clock = Objects.requireNonNull(clock);
        this.metrics = Objects.requireNonNull(metrics);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CouponRedemptionResult redeem(String normalizedCode, UserId userId, CountryCode resolvedCountry) {
        Timer.Sample sample = metrics.startTransactionTimer();
        TransactionOutcome outcome = TransactionOutcome.DATABASE_ERROR;
        try {
            Coupon coupon = repository.findForUpdate(normalizedCode).orElseThrow(CouponNotFoundException::new);
            if (!coupon.countryCode().equals(resolvedCountry)) throw new CountryNotAllowedException();
            if (repository.exists(coupon.id(), userId)) throw new CouponAlreadyRedeemedException();
            if (coupon.currentUses() >= coupon.maxUses()) throw new CouponExhaustedException();
            var id = uuidGenerator.next(); var redeemedAt = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
            repository.insert(id, coupon.id(), userId, resolvedCountry.value(), redeemedAt);
            int currentUses = repository.incrementIfCapacity(coupon.id()).orElseThrow(IllegalStateException::new);
            outcome = TransactionOutcome.SUCCESS;
            return new CouponRedemptionResult(id, coupon.code().value(), userId.value(), redeemedAt, coupon.maxUses() - currentUses);
        } catch (CouponNotFoundException exception) {
            outcome = TransactionOutcome.NOT_FOUND;
            throw exception;
        } catch (CountryNotAllowedException exception) {
            outcome = TransactionOutcome.COUNTRY_NOT_ALLOWED;
            throw exception;
        } catch (CouponAlreadyRedeemedException exception) {
            outcome = TransactionOutcome.ALREADY_REDEEMED;
            throw exception;
        } catch (CouponExhaustedException exception) {
            outcome = TransactionOutcome.EXHAUSTED;
            throw exception;
        } finally {
            metrics.stopTransactionTimer(sample, outcome);
        }
    }
}
