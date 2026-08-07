package pl.radoslawpiatek.couponservice.geolocation.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.coupon.domain.InvalidCouponValueException;
import pl.radoslawpiatek.couponservice.geolocation.configuration.GeolocationProperties;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;
import pl.radoslawpiatek.couponservice.geolocation.domain.GeolocationUnavailableException;
import pl.radoslawpiatek.couponservice.geolocation.ports.GeoLocationResolver;

/**
 * HTTPS adapter for the ipwho.is demonstration API with bounded, non-retrying request handling.
 *
 * <p>It uses a shared client with redirects disabled and never exposes the raw address, response
 * body, URI or provider message in exceptions. The caller's address exists only as a safe path
 * segment while this method is executing.
 */
public final class IpWhoisGeoLocationResolver implements GeoLocationResolver {

    private static final int MAXIMUM_RESPONSE_BODY_BYTES = 16_384;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI baseUri;
    private final Duration responseTimeout;
    private final int maximumResponseBodyBytes;
    private final PublicIpAddressPolicy publicIpAddressPolicy;

    /**
     * Creates the provider adapter from a shared HTTP client and validated properties.
     *
     * @param httpClient shared client configured with the frozen connect timeout and redirect policy
     * @param objectMapper JSON parser receiving at most the configured bounded body
     * @param properties validated provider configuration
     * @param publicIpAddressPolicy policy preventing special-purpose address egress
     */
    public IpWhoisGeoLocationResolver(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            GeolocationProperties properties,
            PublicIpAddressPolicy publicIpAddressPolicy
    ) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.baseUri = Objects.requireNonNull(properties.baseUri());
        this.responseTimeout = properties.responseTimeout();
        this.maximumResponseBodyBytes = properties.maximumResponseBodyBytes();
        this.publicIpAddressPolicy = Objects.requireNonNull(publicIpAddressPolicy);
        if (maximumResponseBodyBytes != MAXIMUM_RESPONSE_BODY_BYTES) {
            throw new IllegalArgumentException("The GeoIP response limit must be 16384 bytes.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public CountryCode resolve(ClientIpAddress clientIpAddress) {
        if (!publicIpAddressPolicy.permitsPublicLookup(Objects.requireNonNull(clientIpAddress))) {
            throw new GeolocationUnavailableException();
        }
        HttpRequest request = HttpRequest.newBuilder(providerUri(clientIpAddress))
                .GET()
                .timeout(responseTimeout)
                .header("Accept", "application/json")
                .header("Accept-Encoding", "identity")
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                if (response.statusCode() < 200 || response.statusCode() > 299
                        || contentLength > maximumResponseBodyBytes) {
                    throw new GeolocationUnavailableException();
                }
                return parseCountry(readBounded(body));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GeolocationUnavailableException();
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof GeolocationUnavailableException) {
                throw (GeolocationUnavailableException) exception;
            }
            throw new GeolocationUnavailableException();
        }
    }

    private URI providerUri(ClientIpAddress address) {
        String segment = URLEncoder.encode(address.canonicalLiteral(), StandardCharsets.UTF_8).replace("+", "%20");
        String base = baseUri.toString().endsWith("/") ? baseUri.toString() : baseUri + "/";
        return URI.create(base + segment + "?fields=success,country_code,message");
    }

    private byte[] readBounded(InputStream body) throws IOException {
        byte[] bytes = body.readNBytes(maximumResponseBodyBytes + 1);
        if (bytes.length > maximumResponseBodyBytes) {
            throw new GeolocationUnavailableException();
        }
        return bytes;
    }

    private CountryCode parseCountry(byte[] body) {
        try {
            JsonNode document = objectMapper.readTree(body);
            if (document == null || !document.path("success").isBoolean() || !document.path("success").asBoolean()) {
                throw new GeolocationUnavailableException();
            }
            JsonNode country = document.get("country_code");
            if (country == null || !country.isTextual() || !country.textValue().matches("[A-Za-z]{2}")) {
                throw new GeolocationUnavailableException();
            }
            return CountryCode.of(country.textValue());
        } catch (IOException | InvalidCouponValueException exception) {
            throw new GeolocationUnavailableException();
        }
    }
}
