package pl.radoslawpiatek.couponservice.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import pl.radoslawpiatek.couponservice.observability.web.RequestIdFilter;

/** End-to-end evidence for request correlation and the Prometheus scrape boundary. */
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ObservabilityApiIT {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.4-alpine")
            .withDatabaseName("coupon_service")
            .withUsername("coupon_service")
            .withPassword("coupon_service");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("management.endpoints.web.exposure.include", () -> "health,info,prometheus");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private MeterRegistry meters;

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void cleanDatabase() {
        jdbc.sql("TRUNCATE TABLE coupon_redemptions, coupons").update();
    }

    @Test
    void prometheusExposesFrozenMetersWithoutSensitiveLabelValuesAndRequestIdIsReturned() throws Exception {
        String requestId = "observability-it-request";
        HttpResponse<String> created = postJson(
                "/api/v1/coupons",
                "{\"code\":\"OBSERVE\",\"maxUses\":2,\"countryCode\":\"PL\"}",
                requestId
        );
        assertThat(created.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(created.headers().firstValue(RequestIdFilter.HEADER_NAME)).contains(requestId);

        HttpResponse<String> redeemed = postJson(
                "/api/v1/coupons/OBSERVE/redemptions",
                "{\"userId\":\"private-user-987\"}",
                "redeem-observability-request"
        );
        assertThat(redeemed.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(redeemed.headers().firstValue(RequestIdFilter.HEADER_NAME))
                .contains("redeem-observability-request");

        assertThat(meters.get("coupon.create").tag("outcome", "success").counter().count()).isEqualTo(1.0);
        assertThat(meters.get("coupon.redemption").tag("outcome", "success").counter().count()).isEqualTo(1.0);
        assertThat(meters.get("client.ip.resolution").tags("source", "direct", "outcome", "success").counter().count()).isEqualTo(1.0);
        assertThat(meters.get("geolocation.resolution").tags("provider", "stub", "outcome", "success").counter().count()).isEqualTo(1.0);
        assertThat(meters.get("geolocation.provider").tags("provider", "stub", "outcome", "success").timer().count()).isEqualTo(1L);
        assertThat(meters.get("coupon.redemption.transaction").tag("outcome", "success").timer().count()).isEqualTo(1L);

        HttpResponse<String> scrape = get("/actuator/prometheus");
        assertThat(scrape.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(scrape.headers().firstValue("Content-Type")).isPresent();
        assertThat(scrape.headers().firstValue("Content-Type").orElseThrow()).contains("text/plain");
        assertThat(scrape.body())
                .contains("coupon_create_total")
                .contains("coupon_redemption_total")
                .contains("client_ip_resolution_total")
                .contains("geolocation_resolution_total")
                .contains("geolocation_provider_seconds")
                .contains("coupon_redemption_transaction_seconds")
                .doesNotContain("private-user-987")
                .doesNotContain("OBSERVE")
                .doesNotContain(requestId)
                .doesNotContain("redeem-observability-request")
                .doesNotContain("127.0.0.1");
    }

    private HttpResponse<String> postJson(String path, String body, String requestId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header(RequestIdFilter.HEADER_NAME, requestId)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
