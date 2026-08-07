package pl.radoslawpiatek.couponservice.coupon.application;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.stereotype.Service;
import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponCode;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponNotFoundException;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponAlreadyRedeemedException;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponExhaustedException;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryNotAllowedException;
import pl.radoslawpiatek.couponservice.coupon.domain.UserId;
import pl.radoslawpiatek.couponservice.coupon.ports.CouponRedemptionRepository;
import pl.radoslawpiatek.couponservice.geolocation.ports.ClientIpResolver;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpResolutionException;
import pl.radoslawpiatek.couponservice.geolocation.domain.GeolocationUnavailableException;
import pl.radoslawpiatek.couponservice.geolocation.ports.GeoLocationResolver;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics.RedemptionOutcome;

/**
 * Non-transactional redemption orchestrator.
 *
 * <p>It intentionally looks up the coupon before resolving Client IP/GeoIP, returning 404 without
 * egress for an unknown code. It passes only country, canonical code and user ID to the transaction.
 */
@Service
public class RedeemCouponService implements RedeemCouponUseCase {
    private final CouponRedemptionRepository repository;
    private final ClientIpResolver clientIpResolver;
    private final GeoLocationResolver geoLocationResolver;
    private final CouponRedemptionTransaction transaction;
    private final CouponServiceMetrics metrics;

    /**
     * Creates the ordered orchestrator from persistence, trust-boundary and transactional ports.
     *
     * @param repository non-locking snapshot access used before network-dependent geolocation
     * @param clientIpResolver trusted transport-boundary resolver executed before the transaction
     * @param geoLocationResolver provider-neutral country resolver executed before the transaction
     * @param transaction short row-locking redemption transaction that receives no raw IP data
     * @param metrics low-cardinality terminal outcome metrics
     */
    public RedeemCouponService(CouponRedemptionRepository repository, ClientIpResolver clientIpResolver,
                               GeoLocationResolver geoLocationResolver, CouponRedemptionTransaction transaction,
                               CouponServiceMetrics metrics) {
        this.repository = Objects.requireNonNull(repository); this.clientIpResolver = Objects.requireNonNull(clientIpResolver);
        this.geoLocationResolver = Objects.requireNonNull(geoLocationResolver); this.transaction = Objects.requireNonNull(transaction);
        this.metrics = Objects.requireNonNull(metrics);
    }

    /** {@inheritDoc} */
    @Override
    public CouponRedemptionResult redeem(RedeemCouponCommand command, HttpServletRequest request) {
        Objects.requireNonNull(command); Objects.requireNonNull(request);
        CouponCode code = CouponCode.of(command.code());
        UserId userId = UserId.of(command.userId());
        try {
            Coupon snapshot = repository.findSnapshot(code.normalizedValue()).orElseThrow(CouponNotFoundException::new);
            CountryCode resolvedCountry = geoLocationResolver.resolve(clientIpResolver.resolve(request));
            if (!snapshot.countryCode().equals(resolvedCountry)) throw new CountryNotAllowedException();
            CouponRedemptionResult result = transaction.redeem(code.normalizedValue(), userId, resolvedCountry);
            metrics.recordRedemption(RedemptionOutcome.SUCCESS);
            return result;
        } catch (CouponNotFoundException exception) {
            metrics.recordRedemption(RedemptionOutcome.NOT_FOUND);
            throw exception;
        } catch (CountryNotAllowedException exception) {
            metrics.recordRedemption(RedemptionOutcome.COUNTRY_NOT_ALLOWED);
            throw exception;
        } catch (CouponAlreadyRedeemedException exception) {
            metrics.recordRedemption(RedemptionOutcome.ALREADY_REDEEMED);
            throw exception;
        } catch (CouponExhaustedException exception) {
            metrics.recordRedemption(RedemptionOutcome.EXHAUSTED);
            throw exception;
        } catch (ClientIpResolutionException | GeolocationUnavailableException exception) {
            metrics.recordRedemption(RedemptionOutcome.GEOLOCATION_UNAVAILABLE);
            throw exception;
        } catch (RuntimeException exception) {
            metrics.recordRedemption(RedemptionOutcome.INTERNAL_ERROR);
            throw exception;
        }
    }
}
