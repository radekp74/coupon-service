package pl.radoslawpiatek.couponservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiDocumentationIT {

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
    private SwaggerUiConfigProperties swaggerUiConfig;

    @Test
    void servesTheVersionedOpenApiContractFromTheApplicationArtifact() {
        ResponseEntity<String> response = restTemplate.getForEntity("/openapi.yaml", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("openapi: 3.1.0")
                .contains("operationId: createCoupon")
                .contains("COUPON_CODE_CONFLICT")
                .contains("/api/v1/coupons/{code}/redemptions")
                .contains("operationId: redeemCoupon")
                .contains("COUPON_ALREADY_REDEEMED")
                .contains("^[!-~]{1,128}$");
    }

    @Test
    void exposesSwaggerUiConfiguredForTheVersionedContract() {
        ResponseEntity<String> ui = restTemplate.getForEntity("/swagger-ui/index.html", String.class);
        ResponseEntity<Map> configuration = restTemplate.getForEntity(
                "/v3/api-docs/swagger-config",
                Map.class
        );

        assertThat(ui.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ui.getBody()).containsIgnoringCase("swagger ui");

        assertThat(swaggerUiConfig.getUrl()).isEqualTo("/openapi.yaml");
        assertThat(configuration.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(configuration.getBody()).containsEntry("url", "/openapi.yaml");
        assertThat(configuration.getBody().toString()).doesNotContain("petstore");
    }
}
