package pl.radoslawpiatek.couponservice.geolocation.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import pl.radoslawpiatek.couponservice.geolocation.adapters.StubGeoLocationResolver;
import pl.radoslawpiatek.couponservice.geolocation.ports.GeoLocationResolver;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics;

class GeolocationConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(CouponServiceMetrics.class, () -> new CouponServiceMetrics(new SimpleMeterRegistry()))
            .withUserConfiguration(GeolocationConfiguration.class, JacksonAutoConfiguration.class)
            .withPropertyValues(
                    "coupon.client-ip.mode=direct",
                    "coupon.client-ip.max-forwarded-hops=20",
                    "coupon.client-ip.max-header-length=4096",
                    "coupon.geolocation.base-uri=https://ipwho.is",
                    "coupon.geolocation.connect-timeout=500ms",
                    "coupon.geolocation.response-timeout=1s",
                    "coupon.geolocation.maximum-response-body-bytes=16384",
                    "coupon.geolocation.stub-country=PL"
            );

    @Test
    void ipwhoisStartsWithoutPerformingAProviderRequest() {
        contextRunner.withPropertyValues("coupon.geolocation.provider=ipwhois")
                .run(context -> assertThat(context).hasNotFailed()
                        .hasSingleBean(GeoLocationResolver.class));
    }

    @Test
    void stubRequiresLocalOrTestProfile() {
        contextRunner.withPropertyValues("coupon.geolocation.provider=stub")
                .run(context -> assertThat(context).hasFailed());

        contextRunner.withInitializer(context -> context.getEnvironment().setActiveProfiles("test"))
                .withPropertyValues("coupon.geolocation.provider=stub")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(GeoLocationResolver.class)).isInstanceOf(StubGeoLocationResolver.class);
                });
    }

    @Test
    void trustedProxyRequiresValidCidrAndHttpProviderUriIsRejected() {
        contextRunner.withPropertyValues(
                        "coupon.geolocation.provider=ipwhois",
                        "coupon.client-ip.mode=trusted-proxy"
                )
                .run(context -> assertThat(context).hasFailed());
        contextRunner.withPropertyValues(
                        "coupon.geolocation.provider=ipwhois",
                        "coupon.client-ip.mode=trusted-proxy",
                        "coupon.client-ip.trusted-proxies=not-a-cidr"
                )
                .run(context -> assertThat(context).hasFailed());
        contextRunner.withPropertyValues(
                        "coupon.geolocation.provider=ipwhois",
                        "coupon.geolocation.base-uri=http://not-allowed.example"
                )
                .run(context -> assertThat(context).hasFailed());
    }
}
