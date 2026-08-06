package pl.radoslawpiatek.couponservice.coupon.adapters.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** HTTP integration coverage for the stable redemption contract using the test-only GeoIP stub. */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponRedemptionApiIT {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.4-alpine")
            .withDatabaseName("coupon_service").withUsername("coupon_service").withPassword("coupon_service");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl); r.add("spring.datasource.username", POSTGRES::getUsername); r.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired TestRestTemplate rest;
    @Autowired JdbcClient jdbc;
    @BeforeEach void clear() { jdbc.sql("TRUNCATE TABLE coupon_redemptions, coupons").update(); }

    @Test void redeemsRetriesAndExhaustsWithoutLeakingTechnicalDetails() {
        create("REDEEM", 2);
        assertThat(redeem("REDEEM", "user-A", CouponRedemptionResponse.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ResponseEntity<Map> duplicate = redeem("REDEEM", "user-A", Map.class);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(duplicate.getBody()).containsEntry("code", "COUPON_ALREADY_REDEEMED");
        assertThat(redeem("REDEEM", "user-B", CouponRedemptionResponse.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(redeem("REDEEM", "user-C", Map.class).getBody()).containsEntry("code", "COUPON_EXHAUSTED");
        assertThat(jdbc.sql("SELECT current_uses FROM coupons WHERE normalized_code='REDEEM'").query(Integer.class).single()).isEqualTo(2);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM coupon_redemptions").query(Long.class).single()).isEqualTo(2L);
    }

    @Test void rejectsInvalidUserAndDoesNotLookupMissingCouponThroughGeoIp() {
        ResponseEntity<Map> invalid = redeem("MISSING", "has space", Map.class);
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.getBody()).containsEntry("code", "INVALID_REQUEST");
        assertThat(redeem("MISSING", "valid-user", Map.class).getBody()).containsEntry("code", "COUPON_NOT_FOUND");
    }

    @Test void concurrentUsersRespectExactCapacityInThreeRounds() throws Exception {
        for (int round = 0; round < 3; round++) {
            String code = "RACE" + round;
            create(code, 10);
            ExecutorService executor = Executors.newFixedThreadPool(100);
            CountDownLatch ready = new CountDownLatch(100);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<HttpStatusCode>> futures = new ArrayList<>();
            try {
                for (int index = 0; index < 100; index++) {
                    String user = "race-" + round + "-" + index;
                    futures.add(executor.submit(() -> { ready.countDown(); assertThat(start.await(15, TimeUnit.SECONDS)).isTrue(); return redeem(code, user, Map.class).getStatusCode(); }));
                }
                assertThat(ready.await(15, TimeUnit.SECONDS)).isTrue(); start.countDown();
                int created=0, exhausted=0;
                for (Future<HttpStatusCode> future : futures) {
                    HttpStatusCode status=future.get(60, TimeUnit.SECONDS);
                    if (status == HttpStatus.CREATED) created++; else if (status == HttpStatus.CONFLICT) exhausted++; else throw new AssertionError(status);
                }
                assertThat(created).isEqualTo(10); assertThat(exhausted).isEqualTo(90);
                assertThat(jdbc.sql("SELECT current_uses FROM coupons WHERE normalized_code=:code").param("code", code).query(Integer.class).single()).isEqualTo(10);
                assertThat(jdbc.sql("SELECT COUNT(*) FROM coupon_redemptions r JOIN coupons c ON c.id=r.coupon_id WHERE c.normalized_code=:code").param("code", code).query(Long.class).single()).isEqualTo(10L);
            } finally { start.countDown(); executor.shutdownNow(); assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue(); }
        }
    }

    private void create(String code, int uses) { rest.postForEntity("/api/v1/coupons", Map.of("code", code, "maxUses", uses, "countryCode", "PL"), Map.class); }
    private <T> ResponseEntity<T> redeem(String code, String userId, Class<T> type) {
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange("/api/v1/coupons/{code}/redemptions", HttpMethod.POST, new HttpEntity<>(Map.of("userId", userId), headers), type, code);
    }
}
