package pl.radoslawpiatek.couponservice.coupon.adapters.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;
import pl.radoslawpiatek.couponservice.geolocation.domain.GeolocationUnavailableException;
import pl.radoslawpiatek.couponservice.geolocation.ports.ClientIpResolver;
import pl.radoslawpiatek.couponservice.geolocation.ports.GeoLocationResolver;

/** Verifies that redemption's GeoIP decisions have privacy-safe HTTP representations. */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponRedemptionErrorApiIT {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.4-alpine")
            .withDatabaseName("coupon_service").withUsername("coupon_service").withPassword("coupon_service");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired TestRestTemplate rest;
    @Autowired JdbcClient jdbc;
    @MockBean ClientIpResolver clientIpResolver;
    @MockBean GeoLocationResolver geoLocationResolver;

    @BeforeEach
    void clearAndSetSafeDefaults() {
        jdbc.sql("TRUNCATE TABLE coupon_redemptions, coupons").update();
        when(clientIpResolver.resolve(any())).thenReturn(ClientIpAddress.parseLiteral("8.8.8.8"));
        when(geoLocationResolver.resolve(any())).thenReturn(CountryCode.of("PL"));
    }

    @Test
    void wrongCountryReturns403WithoutLeakingUserOrCountryDetails() {
        create("COUNTRY", "PL");
        when(geoLocationResolver.resolve(any())).thenReturn(CountryCode.of("DE"));

        ResponseEntity<Map> response = redeem("COUNTRY", "private-user", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("code", "COUNTRY_NOT_ALLOWED");
        assertThat(response.getBody().toString()).doesNotContain("private-user", "8.8.8.8", "PL", "DE");
        assertInvariant("COUNTRY", 0, 0);
    }

    @Test
    void unavailableGeoIpReturns503WithoutLeakingInfrastructureDetails() {
        create("GEOFAIL", "PL");
        when(geoLocationResolver.resolve(any())).thenThrow(new GeolocationUnavailableException());

        ResponseEntity<Map> response = redeem("GEOFAIL", "private-user", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("code", "GEOLOCATION_UNAVAILABLE");
        assertThat(response.getBody().toString()).doesNotContain("private-user", "8.8.8.8", "provider", "SQL");
        assertInvariant("GEOFAIL", 0, 0);
    }

    private void create(String code, String country) {
        rest.postForEntity("/api/v1/coupons", Map.of("code", code, "maxUses", 2, "countryCode", country), Map.class);
    }

    private <T> ResponseEntity<T> redeem(String code, String userId, Class<T> type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange("/api/v1/coupons/{code}/redemptions", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", userId), headers), type, code);
    }

    private void assertInvariant(String code, int expectedUses, long expectedRedemptions) {
        assertThat(jdbc.sql("SELECT current_uses FROM coupons WHERE normalized_code=:code").param("code", code)
                .query(Integer.class).single()).isEqualTo(expectedUses);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM coupon_redemptions r JOIN coupons c ON c.id=r.coupon_id WHERE c.normalized_code=:code")
                .param("code", code).query(Long.class).single()).isEqualTo(expectedRedemptions);
    }
}
