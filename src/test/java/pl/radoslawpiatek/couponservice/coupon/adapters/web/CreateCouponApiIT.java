package pl.radoslawpiatek.couponservice.coupon.adapters.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreateCouponApiIT {

    private static final int CONCURRENT_ATTEMPTS = 24;

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
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void cleanDatabase() {
        jdbcClient.sql("TRUNCATE TABLE coupon_redemptions, coupons").update();
    }

    @Test
    void createsCouponAndReturnsTheFrozenResponseContract() {
        ResponseEntity<CouponResponse> response = createCoupon(" wiosna ", 100, "pl", CouponResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("wiosna");
        assertThat(response.getBody().maxUses()).isEqualTo(100);
        assertThat(response.getBody().currentUses()).isZero();
        assertThat(response.getBody().countryCode()).isEqualTo("PL");
        assertThat(response.getBody().createdAt()).isNotNull();

        Map<String, Object> row = jdbcClient.sql("""
                SELECT code, normalized_code, max_uses, current_uses, country_code
                FROM coupons
                WHERE id = :id
                """)
                .param("id", response.getBody().id())
                .query()
                .singleRow();

        assertThat(row)
                .containsEntry("code", "wiosna")
                .containsEntry("normalized_code", "WIOSNA")
                .containsEntry("max_uses", 100)
                .containsEntry("current_uses", 0)
                .containsEntry("country_code", "PL");
    }

    @Test
    void returnsProblemDetailsForCaseInsensitiveDuplicateCode() {
        assertThat(createCoupon("WIOSNA", 10, "PL", CouponResponse.class).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map> conflict = createCoupon("wiosna", 10, "PL", Map.class);

        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(conflict.getBody()).containsEntry("status", 409);
        assertThat(conflict.getBody()).containsEntry("code", "COUPON_CODE_CONFLICT");
        assertThat(conflict.getBody().toString()).doesNotContain("uq_coupons", "normalized_code", "SQLException");
    }

    @Test
    void rejectsInvalidCouponRequestWithoutWritingToTheDatabase() {
        ResponseEntity<Map> response = createCoupon("x", 0, "XX", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "INVALID_REQUEST");
        assertThat(couponCount()).isZero();
    }

    @Test
    void concurrentCaseVariantsProduceExactlyOneCreatedCoupon() throws Exception {
        for (int round = 0; round < 3; round++) {
            String baseCode = "race-" + UUID.randomUUID().toString().substring(0, 8);
            ConcurrentResult result = executeConcurrentCreate(baseCode);

            assertThat(result.created()).isEqualTo(1);
            assertThat(result.conflicts()).isEqualTo(CONCURRENT_ATTEMPTS - 1);
            assertThat(result.unexpectedStatuses()).isEmpty();

            Long persisted = jdbcClient.sql("""
                    SELECT COUNT(*)
                    FROM coupons
                    WHERE normalized_code = :normalizedCode
                    """)
                    .param("normalizedCode", baseCode.toUpperCase(Locale.ROOT))
                    .query(Long.class)
                    .single();
            assertThat(persisted).isEqualTo(1L);
        }
    }

    private ConcurrentResult executeConcurrentCreate(String baseCode) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_ATTEMPTS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_ATTEMPTS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<HttpStatusCode>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < CONCURRENT_ATTEMPTS; index++) {
                String variant = index % 2 == 0
                        ? baseCode.toUpperCase(Locale.ROOT)
                        : baseCode.toLowerCase(Locale.ROOT);
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent start barrier timed out");
                    }
                    return createCoupon(variant, 10, "PL", Map.class).getStatusCode();
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int created = 0;
            int conflicts = 0;
            List<HttpStatusCode> unexpected = new ArrayList<>();
            for (Future<HttpStatusCode> future : futures) {
                HttpStatusCode status = future.get(Duration.ofSeconds(30).toMillis(), TimeUnit.MILLISECONDS);
                if (status == HttpStatus.CREATED) {
                    created++;
                } else if (status == HttpStatus.CONFLICT) {
                    conflicts++;
                } else {
                    unexpected.add(status);
                }
            }
            return new ConcurrentResult(created, conflicts, unexpected);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private long couponCount() {
        return jdbcClient.sql("SELECT COUNT(*) FROM coupons")
                .query(Long.class)
                .single();
    }

    private <T> ResponseEntity<T> createCoupon(
            String code,
            int maxUses,
            String countryCode,
            Class<T> responseType
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "code", code,
                "maxUses", maxUses,
                "countryCode", countryCode
        );
        return restTemplate.exchange(
                "/api/v1/coupons",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                responseType
        );
    }

    private record ConcurrentResult(int created, int conflicts, List<HttpStatusCode> unexpectedStatuses) {
    }
}
