package pl.radoslawpiatek.couponservice.geolocation.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.coupon.domain.InvalidCouponValueException;
import pl.radoslawpiatek.couponservice.geolocation.configuration.GeolocationProperties;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;
import pl.radoslawpiatek.couponservice.geolocation.domain.GeolocationUnavailableException;
import pl.radoslawpiatek.couponservice.geolocation.ports.GeoLocationResolver;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics.GeoProvider;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics.GeolocationOutcome;

/**
 * HTTPS adapter for the ipwho.is demonstration API with bounded, non-retrying request handling.
 *
 * <p>It uses a shared client with redirects disabled and never exposes the raw address, response
 * body, URI or provider message in exceptions. The caller's address exists only as a safe path
 * segment while this method is executing. Metrics classify only a closed outcome vocabulary.
 */
public final class IpWhoisGeoLocationResolver implements GeoLocationResolver {

    private static final int MAXIMUM_RESPONSE_BODY_BYTES = 16_384;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI baseUri;
    private final Duration responseTimeout;
    private final int maximumResponseBodyBytes;
    private final PublicIpAddressPolicy publicIpAddressPolicy;
    private final CouponServiceMetrics metrics;

    /**
     * Creates the provider adapter from a shared HTTP client and validated properties.
     *
     * @param httpClient shared client configured with the frozen connect timeout and redirect policy
     * @param objectMapper JSON parser receiving at most the configured bounded body
     * @param properties validated provider configuration
     * @param publicIpAddressPolicy policy preventing special-purpose address egress
     * @param metrics low-cardinality provider outcome metrics
     */
    public IpWhoisGeoLocationResolver(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            GeolocationProperties properties,
            PublicIpAddressPolicy publicIpAddressPolicy,
            CouponServiceMetrics metrics
    ) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.baseUri = Objects.requireNonNull(properties.baseUri());
        this.responseTimeout = properties.responseTimeout();
        this.maximumResponseBodyBytes = properties.maximumResponseBodyBytes();
        this.publicIpAddressPolicy = Objects.requireNonNull(publicIpAddressPolicy);
        this.metrics = Objects.requireNonNull(metrics);
        if (maximumResponseBodyBytes != MAXIMUM_RESPONSE_BODY_BYTES) {
            throw new IllegalArgumentException("The GeoIP response limit must be 16384 bytes.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public CountryCode resolve(ClientIpAddress clientIpAddress) {
        ClientIpAddress address = Objects.requireNonNull(clientIpAddress);
        if (!publicIpAddressPolicy.permitsPublicLookup(address)) {
            metrics.recordGeolocation(GeoProvider.IPWHOIS, GeolocationOutcome.NON_PUBLIC_IP);
            throw new GeolocationUnavailableException();
        }

        Timer.Sample sample = metrics.startGeolocationTimer();
        GeolocationOutcome outcome = GeolocationOutcome.PROVIDER_ERROR;
        try {
            HttpRequest request = HttpRequest.newBuilder(providerUri(address))
                    .GET()
                    .timeout(responseTimeout)
                    .header("Accept", "application/json")
                    .header("Accept-Encoding", "identity")
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() == 429) {
                    outcome = GeolocationOutcome.RATE_LIMITED;
                    throw new GeolocationUnavailableException();
                }
                long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                if (response.statusCode() < 200 || response.statusCode() > 299) {
                    outcome = GeolocationOutcome.PROVIDER_ERROR;
                    throw new GeolocationUnavailableException();
                }
                if (contentLength > maximumResponseBodyBytes) {
                    outcome = GeolocationOutcome.INVALID_RESPONSE;
                    throw new GeolocationUnavailableException();
                }
                ProviderResult providerResult = parseCountry(readBounded(body));
                outcome = providerResult.outcome();
                if (providerResult.countryCode() == null) {
                    throw new GeolocationUnavailableException();
                }
                return providerResult.countryCode();
            }
        } catch (HttpTimeoutException exception) {
            outcome = GeolocationOutcome.TIMEOUT;
            throw unavailable();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            outcome = GeolocationOutcome.PROVIDER_ERROR;
            throw unavailable();
        } catch (IOException exception) {
            outcome = GeolocationOutcome.PROVIDER_ERROR;
            throw unavailable();
        } catch (GeolocationUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            outcome = GeolocationOutcome.INVALID_RESPONSE;
            throw unavailable();
        } finally {
            metrics.recordGeolocation(GeoProvider.IPWHOIS, outcome);
            metrics.stopGeolocationTimer(sample, GeoProvider.IPWHOIS, outcome);
        }
    }

    private GeolocationUnavailableException unavailable() {
        return new GeolocationUnavailableException();
    }

    private URI providerUri(ClientIpAddress address) {
        String segment = URLEncoder.encode(address.canonicalLiteral(), StandardCharsets.UTF_8).replace("+", "%20");
        String base = baseUri.toString().endsWith("/") ? baseUri.toString() : baseUri + "/";
        return URI.create(base + segment + "?fields=success,country_code,message");
    }

    private byte[] readBounded(InputStream body) throws IOException {
        byte[] bytes = body.readNBytes(maximumResponseBodyBytes + 1);
        if (bytes.length > maximumResponseBodyBytes) {
            throw new InvalidProviderResponseException();
        }
        return bytes;
    }

    private ProviderResult parseCountry(byte[] body) {
        try {
            JsonNode document = objectMapper.readTree(body);
            if (document == null || !document.path("success").isBoolean()) {
                throw new InvalidProviderResponseException();
            }
            if (!document.path("success").asBoolean()) {
                return new ProviderResult(null, GeolocationOutcome.PROVIDER_ERROR);
            }
            JsonNode country = document.get("country_code");
            if (country == null || !country.isTextual() || !country.textValue().matches("[A-Za-z]{2}")) {
                throw new InvalidProviderResponseException();
            }
            return new ProviderResult(CountryCode.of(country.textValue()), GeolocationOutcome.SUCCESS);
        } catch (IOException | InvalidCouponValueException exception) {
            throw new InvalidProviderResponseException();
        }
    }

    private record ProviderResult(CountryCode countryCode, GeolocationOutcome outcome) {
    }

    private static final class InvalidProviderResponseException extends RuntimeException {
        private InvalidProviderResponseException() {
            super(null, null, false, false);
        }
    }
}
