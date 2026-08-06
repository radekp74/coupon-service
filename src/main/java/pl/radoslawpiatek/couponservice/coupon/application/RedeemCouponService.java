package pl.radoslawpiatek.couponservice.coupon.application;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.stereotype.Service;
import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponCode;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponNotFoundException;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryNotAllowedException;
import pl.radoslawpiatek.couponservice.coupon.domain.UserId;
import pl.radoslawpiatek.couponservice.coupon.ports.CouponRedemptionRepository;
import pl.radoslawpiatek.couponservice.geolocation.ports.ClientIpResolver;
import pl.radoslawpiatek.couponservice.geolocation.ports.GeoLocationResolver;

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

    /** Creates the ordered orchestrator from persistence, trust-boundary and transactional ports. */
    public RedeemCouponService(CouponRedemptionRepository repository, ClientIpResolver clientIpResolver,
                               GeoLocationResolver geoLocationResolver, CouponRedemptionTransaction transaction) {
        this.repository = Objects.requireNonNull(repository); this.clientIpResolver = Objects.requireNonNull(clientIpResolver);
        this.geoLocationResolver = Objects.requireNonNull(geoLocationResolver); this.transaction = Objects.requireNonNull(transaction);
    }

    @Override
    public CouponRedemptionResult redeem(RedeemCouponCommand command, HttpServletRequest request) {
        Objects.requireNonNull(command); Objects.requireNonNull(request);
        CouponCode code = CouponCode.of(command.code());
        UserId userId = UserId.of(command.userId());
        Coupon snapshot = repository.findSnapshot(code.normalizedValue()).orElseThrow(CouponNotFoundException::new);
        CountryCode resolvedCountry = geoLocationResolver.resolve(clientIpResolver.resolve(request));
        if (!snapshot.countryCode().equals(resolvedCountry)) throw new CountryNotAllowedException();
        return transaction.redeem(code.normalizedValue(), userId, resolvedCountry);
    }
}
