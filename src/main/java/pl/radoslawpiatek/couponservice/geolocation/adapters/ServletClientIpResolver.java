package pl.radoslawpiatek.couponservice.geolocation.adapters;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import pl.radoslawpiatek.couponservice.geolocation.configuration.ClientIpProperties;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpResolutionException;
import pl.radoslawpiatek.couponservice.geolocation.ports.ClientIpResolver;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics.ClientIpOutcome;
import pl.radoslawpiatek.couponservice.observability.CouponServiceMetrics.ClientIpSource;

/**
 * Servlet adapter implementing the direct and trusted-proxy client-address contracts.
 *
 * <p>In direct mode every forwarding header is ignored. In trusted-proxy mode physical header
 * field-lines are counted through {@link HttpServletRequest#getHeaders(String)} before precedence
 * is applied, preventing ambiguity from being silently merged.
 */
public final class ServletClientIpResolver implements ClientIpResolver {

    private final ClientIpProperties properties;
    private final List<CidrBlock> trustedProxies;
    private final ForwardedHeaderParser forwardedHeaderParser = new ForwardedHeaderParser();
    private final XForwardedForParser xForwardedForParser = new XForwardedForParser();
    private final CouponServiceMetrics metrics;

    /**
     * Creates the resolver from configuration already validated during application startup.
     *
     * @param properties immutable security boundary configuration
     * @param metrics low-cardinality client-IP resolution metrics
     */
    public ServletClientIpResolver(ClientIpProperties properties, CouponServiceMetrics metrics) {
        this.properties = Objects.requireNonNull(properties);
        this.trustedProxies = properties.trustedProxies().stream().map(CidrBlock::parse).toList();
        this.metrics = Objects.requireNonNull(metrics);
    }

    /** {@inheritDoc} */
    @Override
    public ClientIpAddress resolve(HttpServletRequest request) {
        Objects.requireNonNull(request);
        ClientIpSource source = ClientIpSource.DIRECT;
        try {
            ClientIpAddress immediatePeer = ClientIpAddress.parseLiteral(request.getRemoteAddr());
            if (properties.mode() == ClientIpProperties.Mode.DIRECT || !isTrusted(immediatePeer)) {
                metrics.recordClientIp(source, ClientIpOutcome.SUCCESS);
                return immediatePeer;
            }

            List<String> forwarded = physicalHeaderValues(request.getHeaders("Forwarded"));
            List<String> xForwardedFor = physicalHeaderValues(request.getHeaders("X-Forwarded-For"));
            if (!forwarded.isEmpty()) {
                source = ClientIpSource.FORWARDED;
            } else if (!xForwardedFor.isEmpty()) {
                source = ClientIpSource.X_FORWARDED_FOR;
            }
            if (forwarded.size() > 1 || xForwardedFor.size() > 1
                    || forwarded.stream().anyMatch(value -> value == null || value.isBlank())
                    || xForwardedFor.stream().anyMatch(value -> value == null || value.isBlank())) {
                throw new ClientIpResolutionException();
            }

            List<ClientIpAddress> chain;
            if (!forwarded.isEmpty()) {
                chain = forwardedHeaderParser.parse(
                        forwarded.getFirst(), properties.maxHeaderLength(), properties.maxForwardedHops());
            } else if (!xForwardedFor.isEmpty()) {
                chain = xForwardedForParser.parse(
                        xForwardedFor.getFirst(), properties.maxHeaderLength(), properties.maxForwardedHops());
            } else {
                throw new ClientIpResolutionException();
            }

            List<ClientIpAddress> completeChain = new ArrayList<>(chain);
            completeChain.add(immediatePeer);
            for (int index = completeChain.size() - 1; index >= 0; index--) {
                ClientIpAddress candidate = completeChain.get(index);
                if (!isTrusted(candidate)) {
                    metrics.recordClientIp(source, ClientIpOutcome.SUCCESS);
                    return candidate;
                }
            }
            throw new ClientIpResolutionException();
        } catch (RuntimeException exception) {
            metrics.recordClientIp(source, ClientIpOutcome.FAILURE);
            throw exception;
        }
    }

    private boolean isTrusted(ClientIpAddress address) {
        return trustedProxies.stream().anyMatch(cidr -> cidr.contains(address));
    }

    private List<String> physicalHeaderValues(Enumeration<String> values) {
        if (values == null) {
            return List.of();
        }
        return Collections.list(values);
    }
}
