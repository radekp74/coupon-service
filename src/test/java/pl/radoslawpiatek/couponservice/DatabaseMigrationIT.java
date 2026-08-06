package pl.radoslawpiatek.couponservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DatabaseMigrationIT {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.4-alpine")
        .withDatabaseName("coupon_service")
        .withUsername("coupon_service")
        .withPassword("coupon_service");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void flywayCreatesTheCouponSchemaOnPostgreSql() {
        Long businessTableCount = jdbcClient.sql("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('coupons', 'coupon_redemptions')
                """)
            .query(Long.class)
            .single();

        assertThat(businessTableCount).isEqualTo(2L);

        Long migrationCount = jdbcClient.sql("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = TRUE
                """)
            .query(Long.class)
            .single();

        assertThat(migrationCount).isEqualTo(1L);
    }

    @Test
    void databaseEnforcesCaseInsensitiveCanonicalCodeUniqueness() {
        insertCoupon(UUID.randomUUID(), "WIOSNA", "WIOSNA");

        assertThatThrownBy(() -> insertCoupon(UUID.randomUUID(), "wiosna", "WIOSNA"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsAnInconsistentCanonicalCode() {
        assertThatThrownBy(() -> insertCoupon(UUID.randomUUID(), "wiosna", "SPRING"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsUsageCountAboveTheCouponLimit() {
        UUID couponId = UUID.randomUUID();
        insertCoupon(couponId, "LIMIT-ONE", "LIMIT-ONE");

        assertThatThrownBy(() -> jdbcClient.sql("""
                UPDATE coupons
                SET current_uses = 2
                WHERE id = :id
                """)
            .param("id", couponId)
            .update())
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertCoupon(UUID id, String code, String normalizedCode) {
        jdbcClient.sql("""
                INSERT INTO coupons (
                    id,
                    code,
                    normalized_code,
                    max_uses,
                    country_code
                ) VALUES (
                    :id,
                    :code,
                    :normalizedCode,
                    1,
                    'PL'
                )
                """)
            .param("id", id)
            .param("code", code)
            .param("normalizedCode", normalizedCode)
            .update();
    }
}
