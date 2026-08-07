package pl.radoslawpiatek.couponservice.observability.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void reusesOneValidIncomingValueAndAlwaysCleansMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER_NAME, "req-ABC_123.4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isEqualTo("req-ABC_123.4"));

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("req-ABC_123.4");
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void missingInvalidAndMultipleValuesGenerateFreshUuidsWithoutRejectingRequests() throws Exception {
        for (MockHttpServletRequest request : new MockHttpServletRequest[] {
                new MockHttpServletRequest(),
                requestWithHeader("bad value"),
                requestWithTwoHeaders("first", "second")
        }) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });
            String generated = response.getHeader(RequestIdFilter.HEADER_NAME);
            assertThat(generated).isNotBlank();
            assertThat(UUID.fromString(generated).toString()).isEqualTo(generated);
            assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
        }
    }

    @Test
    void cleanupRunsEvenWhenDownstreamFails() {
        MockHttpServletRequest request = requestWithHeader("request-ok");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isEqualTo("request-ok");
            throw new ServletException("synthetic downstream failure");
        })).isInstanceOf(ServletException.class);

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("request-ok");
    }

    private MockHttpServletRequest requestWithHeader(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER_NAME, value);
        return request;
    }

    private MockHttpServletRequest requestWithTwoHeaders(String first, String second) {
        MockHttpServletRequest request = requestWithHeader(first);
        request.addHeader(RequestIdFilter.HEADER_NAME, second);
        return request;
    }
}
