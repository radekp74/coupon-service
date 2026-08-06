package pl.radoslawpiatek.couponservice.coupon.adapters.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

/** Exercises PostgreSQL rollback guarantees with database objects that exist only for one test. */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponRedemptionRollbackIT {
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

    @BeforeEach
    void clear() {
        jdbc.sql("TRUNCATE TABLE coupon_redemptions, coupons").update();
        dropTestObjects();
    }

    @Test
    void insertFailureRollsBackCounterAndReturnsPrivacySafeInternalError() {
        create("INSERTFAIL", 2);
        jdbc.sql("CREATE FUNCTION fail_redemption_insert_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'injected insert failure'; END; $$").update();
        jdbc.sql("CREATE TRIGGER fail_redemption_insert_test BEFORE INSERT ON coupon_redemptions FOR EACH ROW EXECUTE FUNCTION fail_redemption_insert_test()").update();
        try {
            ResponseEntity<Map> response = redeem("INSERTFAIL", "private-user", Map.class);
            assertInternalError(response);
            assertInvariant("INSERTFAIL", 0, 0);
        } finally {
            dropTestObjects();
        }
    }

    @Test
    void updateFailureAfterInsertRollsBackTheInsertedRedemptionAndAllowsLaterSuccess() {
        create("UPDATEFAIL", 2);
        jdbc.sql("CREATE FUNCTION fail_coupon_update_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'injected update failure'; END; $$").update();
        jdbc.sql("CREATE TRIGGER fail_coupon_update_test BEFORE UPDATE OF current_uses ON coupons FOR EACH ROW EXECUTE FUNCTION fail_coupon_update_test()").update();
        try {
            ResponseEntity<Map> response = redeem("UPDATEFAIL", "private-user", Map.class);
            assertInternalError(response);
            assertInvariant("UPDATEFAIL", 0, 0);
            assertThat(jdbc.sql("SELECT COUNT(*) FROM coupon_redemptions WHERE user_id='private-user'").query(Long.class).single()).isZero();
        } finally {
            dropTestObjects();
        }
        assertThat(redeem("UPDATEFAIL", "private-user", Map.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertInvariant("UPDATEFAIL", 1, 1);
    }

    @Test
    void differentDatabaseConstraintIsInternalErrorNotAlreadyRedeemed() {
        create("OTHER", 2);
        assertThat(redeem("OTHER", "first-user", Map.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        jdbc.sql("ALTER TABLE coupon_redemptions ADD CONSTRAINT uq_test_redemption_country UNIQUE (resolved_country_code)").update();
        create("TARGET", 2);
        try {
            ResponseEntity<Map> response = redeem("TARGET", "private-user", Map.class);
            assertInternalError(response);
            assertThat(response.getBody().toString()).doesNotContain("uq_test_redemption_country", "private-user");
            assertInvariant("TARGET", 0, 0);
        } finally {
            jdbc.sql("ALTER TABLE coupon_redemptions DROP CONSTRAINT IF EXISTS uq_test_redemption_country").update();
        }
    }

    private void create(String code, int uses) {
        rest.postForEntity("/api/v1/coupons", Map.of("code", code, "maxUses", uses, "countryCode", "PL"), Map.class);
    }

    private <T> ResponseEntity<T> redeem(String code, String userId, Class<T> type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange("/api/v1/coupons/{code}/redemptions", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", userId), headers), type, code);
    }

    private void assertInternalError(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("code", "INTERNAL_ERROR");
        assertThat(response.getBody().toString()).doesNotContain("SQL", "trigger", "injected");
    }

    private void assertInvariant(String code, int expectedUses, long expectedRedemptions) {
        assertThat(jdbc.sql("SELECT current_uses FROM coupons WHERE normalized_code=:code").param("code", code)
                .query(Integer.class).single()).isEqualTo(expectedUses);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM coupon_redemptions r JOIN coupons c ON c.id=r.coupon_id WHERE c.normalized_code=:code")
                .param("code", code).query(Long.class).single()).isEqualTo(expectedRedemptions);
    }

    private void dropTestObjects() {
        jdbc.sql("DROP TRIGGER IF EXISTS fail_redemption_insert_test ON coupon_redemptions").update();
        jdbc.sql("DROP TRIGGER IF EXISTS fail_coupon_update_test ON coupons").update();
        jdbc.sql("DROP FUNCTION IF EXISTS fail_redemption_insert_test()").update();
        jdbc.sql("DROP FUNCTION IF EXISTS fail_coupon_update_test()").update();
        jdbc.sql("ALTER TABLE coupon_redemptions DROP CONSTRAINT IF EXISTS uq_test_redemption_country").update();
    }
}
