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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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
    @Autowired PlatformTransactionManager transactionManager;
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

    @Test void sameUserConcurrentRetriesProduceExactlyOneSuccessAndNineteenConflicts() throws Exception {
        create("SAMEUSER", 20);
        List<String> users = new ArrayList<>(); for (int index = 0; index < 20; index++) users.add("same-user");
        List<ResponseEntity<Map>> responses = concurrentResponses("SAMEUSER", users);
        assertThat(responses.stream().filter(response -> response.getStatusCode() == HttpStatus.CREATED).count()).isEqualTo(1);
        assertThat(responses.stream().filter(response -> response.getStatusCode() == HttpStatus.CONFLICT).count()).isEqualTo(19);
        assertThat(responses.stream().filter(response -> response.getStatusCode() == HttpStatus.CONFLICT)
                .map(ResponseEntity::getBody).map(body -> body.get("code"))).containsOnly("COUPON_ALREADY_REDEEMED");
        assertThat(jdbc.sql("SELECT current_uses FROM coupons WHERE normalized_code='SAMEUSER'").query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM coupon_redemptions").query(Long.class).single()).isEqualTo(1L);
    }

    @Test void twoDifferentUsersCompeteForTheLastSlotWithExactOutcomes() throws Exception {
        create("LASTSLOT", 2);
        assertThat(redeem("LASTSLOT", "seed", Map.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        List<ResponseEntity<Map>> responses = concurrentResponses("LASTSLOT", List.of("user-A", "user-B"));
        assertThat(responses.stream().map(ResponseEntity::getStatusCode)).containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
        assertThat(responses.stream().filter(response -> response.getStatusCode() == HttpStatus.CONFLICT)
                .map(ResponseEntity::getBody).map(body -> body.get("code"))).containsOnly("COUPON_EXHAUSTED");
        assertThat(jdbc.sql("SELECT current_uses FROM coupons WHERE normalized_code='LASTSLOT'").query(Integer.class).single()).isEqualTo(2);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM coupon_redemptions").query(Long.class).single()).isEqualTo(2L);
    }

    @Test void rowLockOnOneCouponDoesNotGloballySerializeAnotherCoupon() throws Exception {
        create("LOCKA", 2); create("LOCKB", 2);
        CountDownLatch locked = new CountDownLatch(1); CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Void> holder = executor.submit(() -> {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    jdbc.sql("SELECT id FROM coupons WHERE normalized_code='LOCKA' FOR UPDATE").query(java.util.UUID.class).single();
                    locked.countDown();
                    try { if (!release.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("release timeout"); }
                    catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
                });
                return null;
            });
            assertThat(locked.await(15, TimeUnit.SECONDS)).isTrue();
            Future<ResponseEntity<Map>> independentRedemption = executor.submit(() -> redeem("LOCKB", "lock-free-user", Map.class));
            assertThat(independentRedemption.get(15, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.CREATED);
            release.countDown(); holder.get(15, TimeUnit.SECONDS);
            assertThat(jdbc.sql("SELECT current_uses FROM coupons WHERE normalized_code='LOCKB'").query(Integer.class).single()).isEqualTo(1);
        } finally { release.countDown(); executor.shutdownNow(); assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue(); }
    }

    private List<HttpStatusCode> concurrent(String code, List<String> users) throws Exception {
        ExecutorService executor=Executors.newFixedThreadPool(users.size()); CountDownLatch ready=new CountDownLatch(users.size()); CountDownLatch start=new CountDownLatch(1); List<Future<HttpStatusCode>> futures=new ArrayList<>();
        try { for(String user:users) futures.add(executor.submit(() -> { ready.countDown(); if(!start.await(15,TimeUnit.SECONDS)) throw new IllegalStateException("barrier timeout"); return redeem(code,user,Map.class).getStatusCode(); }));
            assertThat(ready.await(15,TimeUnit.SECONDS)).isTrue(); start.countDown(); List<HttpStatusCode> statuses=new ArrayList<>(); for(Future<HttpStatusCode> future:futures) statuses.add(future.get(60,TimeUnit.SECONDS)); return statuses;
        } finally { start.countDown(); executor.shutdownNow(); assertThat(executor.awaitTermination(15,TimeUnit.SECONDS)).isTrue(); }
    }

    private List<ResponseEntity<Map>> concurrentResponses(String code, List<String> users) throws Exception {
        ExecutorService executor=Executors.newFixedThreadPool(users.size()); CountDownLatch ready=new CountDownLatch(users.size()); CountDownLatch start=new CountDownLatch(1); List<Future<ResponseEntity<Map>>> futures=new ArrayList<>();
        try { for(String user:users) futures.add(executor.submit(() -> { ready.countDown(); if(!start.await(15,TimeUnit.SECONDS)) throw new IllegalStateException("barrier timeout"); return redeem(code,user,Map.class); }));
            assertThat(ready.await(15,TimeUnit.SECONDS)).isTrue(); start.countDown(); List<ResponseEntity<Map>> responses=new ArrayList<>(); for(Future<ResponseEntity<Map>> future:futures) responses.add(future.get(60,TimeUnit.SECONDS)); return responses;
        } finally { start.countDown(); executor.shutdownNow(); assertThat(executor.awaitTermination(15,TimeUnit.SECONDS)).isTrue(); }
    }

    private void create(String code, int uses) { rest.postForEntity("/api/v1/coupons", Map.of("code", code, "maxUses", uses, "countryCode", "PL"), Map.class); }
    private <T> ResponseEntity<T> redeem(String code, String userId, Class<T> type) {
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange("/api/v1/coupons/{code}/redemptions", HttpMethod.POST, new HttpEntity<>(Map.of("userId", userId), headers), type, code);
    }
}
