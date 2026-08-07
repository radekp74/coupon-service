package pl.radoslawpiatek.couponservice.observability.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Establishes a bounded correlation identifier for every HTTP request.
 *
 * <p>Only one physical {@code X-Request-Id} field-line matching the frozen public syntax is
 * trusted. Any missing, repeated or malformed value is replaced with a server UUID. The selected
 * value is returned to the caller and lives in MDC only for the duration of the request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class RequestIdFilter extends OncePerRequestFilter {

    /** Public response/request header carrying the correlation identifier. */
    public static final String HEADER_NAME = "X-Request-Id";
    /** MDC key emitted by Spring Boot structured JSON logging. */
    public static final String MDC_KEY = "requestId";
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$");

    /** Creates the stateless correlation filter used by the servlet chain. */
    public RequestIdFilter() {
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = selectRequestId(request.getHeaders(HEADER_NAME));
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER_NAME, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String selectRequestId(Enumeration<String> values) {
        List<String> physicalValues = values == null ? List.of() : Collections.list(values);
        if (physicalValues.size() == 1 && VALID.matcher(physicalValues.getFirst()).matches()) {
            return physicalValues.getFirst();
        }
        return UUID.randomUUID().toString();
    }
}
