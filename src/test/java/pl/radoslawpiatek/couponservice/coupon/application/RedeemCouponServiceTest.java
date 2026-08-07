package pl.radoslawpiatek.couponservice.coupon.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import pl.radoslawpiatek.couponservice.coupon.domain.*;
import pl.radoslawpiatek.couponservice.coupon.ports.CouponRedemptionRepository;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;
import pl.radoslawpiatek.couponservice.geolocation.domain.GeolocationUnavailableException;
import pl.radoslawpiatek.couponservice.geolocation.ports.ClientIpResolver;
import pl.radoslawpiatek.couponservice.geolocation.ports.GeoLocationResolver;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics;

class RedeemCouponServiceTest {
    @Test void notFoundStopsBeforeClientIpGeoIpAndTransaction() {
        CouponRedemptionRepository repository=mock(CouponRedemptionRepository.class); ClientIpResolver ip=mock(ClientIpResolver.class); GeoLocationResolver geo=mock(GeoLocationResolver.class); CouponRedemptionTransaction tx=mock(CouponRedemptionTransaction.class);
        when(repository.findSnapshot("MISSING")).thenReturn(Optional.empty());
        RedeemCouponService service=new RedeemCouponService(repository,ip,geo,tx,metrics());
        assertThatThrownBy(() -> service.redeem(new RedeemCouponCommand("missing","user"),mock(HttpServletRequest.class))).isInstanceOf(CouponNotFoundException.class);
        verifyNoInteractions(ip,geo,tx);
    }
    @Test void ordersSnapshotIpGeoIpThenTransactionAndPassesNoRawIp() {
        CouponRedemptionRepository repository=mock(CouponRedemptionRepository.class); ClientIpResolver ip=mock(ClientIpResolver.class); GeoLocationResolver geo=mock(GeoLocationResolver.class); CouponRedemptionTransaction tx=mock(CouponRedemptionTransaction.class);
        Coupon coupon=new Coupon(UUID.randomUUID(),CouponCode.of("CODE"),OffsetDateTime.now(),2,0,CountryCode.of("PL"));
        ClientIpAddress address=ClientIpAddress.parseLiteral("8.8.8.8");
        when(repository.findSnapshot("CODE")).thenReturn(Optional.of(coupon)); when(ip.resolve(any())).thenReturn(address); when(geo.resolve(address)).thenReturn(CountryCode.of("PL"));
        when(tx.redeem(eq("CODE"),eq(UserId.of("customer-A")),eq(CountryCode.of("PL")))).thenReturn(new CouponRedemptionResult(UUID.randomUUID(),"CODE","customer-A",OffsetDateTime.now(),1));
        CouponRedemptionResult result = new RedeemCouponService(repository,ip,geo,tx,metrics())
                .redeem(new RedeemCouponCommand("code","customer-A"),mock(HttpServletRequest.class));
        assertThat(result.couponCode()).isEqualTo("CODE"); assertThat(result.userId()).isEqualTo("customer-A");
        InOrder order=inOrder(repository,ip,geo,tx); order.verify(repository).findSnapshot("CODE"); order.verify(ip).resolve(any()); order.verify(geo).resolve(address); order.verify(tx).redeem("CODE",UserId.of("customer-A"),CountryCode.of("PL"));
    }
    @Test void infrastructureAndCountryFailuresNeverCallTransaction() {
        CouponRedemptionRepository repository=mock(CouponRedemptionRepository.class); ClientIpResolver ip=mock(ClientIpResolver.class); GeoLocationResolver geo=mock(GeoLocationResolver.class); CouponRedemptionTransaction tx=mock(CouponRedemptionTransaction.class);
        Coupon coupon=new Coupon(UUID.randomUUID(),CouponCode.of("CODE"),OffsetDateTime.now(),2,0,CountryCode.of("PL")); when(repository.findSnapshot("CODE")).thenReturn(Optional.of(coupon));
        when(ip.resolve(any())).thenThrow(new GeolocationUnavailableException());
        assertThatThrownBy(() -> new RedeemCouponService(repository,ip,geo,tx,metrics()).redeem(new RedeemCouponCommand("code","user"),mock(HttpServletRequest.class))).isInstanceOf(GeolocationUnavailableException.class); verifyNoInteractions(geo,tx);
    }

    @Test void geoIpFailureStopsBeforeTransaction() {
        CouponRedemptionRepository repository=mock(CouponRedemptionRepository.class); ClientIpResolver ip=mock(ClientIpResolver.class); GeoLocationResolver geo=mock(GeoLocationResolver.class); CouponRedemptionTransaction tx=mock(CouponRedemptionTransaction.class);
        when(repository.findSnapshot("CODE")).thenReturn(Optional.of(coupon("PL"))); when(ip.resolve(any())).thenReturn(ClientIpAddress.parseLiteral("8.8.8.8"));
        when(geo.resolve(any())).thenThrow(new GeolocationUnavailableException());
        assertThatThrownBy(() -> new RedeemCouponService(repository,ip,geo,tx,metrics()).redeem(new RedeemCouponCommand("code","user"),mock(HttpServletRequest.class))).isInstanceOf(GeolocationUnavailableException.class);
        verifyNoInteractions(tx);
    }

    @Test void wrongCountryStopsBeforeTransaction() {
        CouponRedemptionRepository repository=mock(CouponRedemptionRepository.class); ClientIpResolver ip=mock(ClientIpResolver.class); GeoLocationResolver geo=mock(GeoLocationResolver.class); CouponRedemptionTransaction tx=mock(CouponRedemptionTransaction.class);
        when(repository.findSnapshot("CODE")).thenReturn(Optional.of(coupon("PL"))); when(ip.resolve(any())).thenReturn(ClientIpAddress.parseLiteral("8.8.8.8")); when(geo.resolve(any())).thenReturn(CountryCode.of("DE"));
        assertThatThrownBy(() -> new RedeemCouponService(repository,ip,geo,tx,metrics()).redeem(new RedeemCouponCommand("code","user"),mock(HttpServletRequest.class))).isInstanceOf(CountryNotAllowedException.class);
        verifyNoInteractions(tx);
    }

    private CouponServiceMetrics metrics() { return new CouponServiceMetrics(new SimpleMeterRegistry()); }

    private Coupon coupon(String country) { return new Coupon(UUID.randomUUID(),CouponCode.of("CODE"),OffsetDateTime.now(),2,0,CountryCode.of(country)); }
}
