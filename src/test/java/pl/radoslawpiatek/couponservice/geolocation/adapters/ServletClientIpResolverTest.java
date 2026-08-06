package pl.radoslawpiatek.couponservice.geolocation.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import pl.radoslawpiatek.couponservice.geolocation.configuration.ClientIpProperties;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpResolutionException;

class ServletClientIpResolverTest {

    @Test
    void directModeIgnoresSpoofedForwardingHeaders() {
        MockHttpServletRequest request = requestFrom("10.0.0.7");
        request.addHeader("Forwarded", "for=8.8.8.8");
        request.addHeader("X-Forwarded-For", "1.1.1.1");

        assertThat(resolver(ClientIpProperties.Mode.DIRECT).resolve(request).canonicalLiteral()).isEqualTo("10.0.0.7");
    }

    @Test
    void untrustedImmediatePeerIgnoresSpoofedHeaders() {
        MockHttpServletRequest request = requestFrom("8.8.8.8");
        request.addHeader("Forwarded", "for=1.1.1.1");

        assertThat(resolver(ClientIpProperties.Mode.TRUSTED_PROXY).resolve(request).canonicalLiteral()).isEqualTo("8.8.8.8");
    }

    @Test
    void trustedChainUsesFirstUntrustedHopFromRight() {
        MockHttpServletRequest request = requestFrom("10.0.0.2");
        request.addHeader("Forwarded", "for=8.8.8.8, for=10.0.0.1");

        assertThat(resolver(ClientIpProperties.Mode.TRUSTED_PROXY).resolve(request).canonicalLiteral()).isEqualTo("8.8.8.8");
    }

    @Test
    void supportsBracketedIpv6WithValidatedPortInForwarded() {
        MockHttpServletRequest request = requestFrom("10.0.0.2");
        request.addHeader("Forwarded", "for=\"[2001:4860:4860::8888]:4711\"");

        assertThat(resolver(ClientIpProperties.Mode.TRUSTED_PROXY).resolve(request).canonicalLiteral()).contains(":");
    }

    @Test
    void rejectsUnsupportedForwardedAndXffAddressForms() {
        for (String header : new String[] {"for=8.8.8.8:443", "for=[2001:db8::1]:0", "for=[2001:db8::1]:65536", "for=_hidden"}) {
            MockHttpServletRequest request = requestFrom("10.0.0.2");
            request.addHeader("Forwarded", header);
            assertThatThrownBy(() -> resolver(ClientIpProperties.Mode.TRUSTED_PROXY).resolve(request))
                    .isInstanceOf(ClientIpResolutionException.class);
        }
        MockHttpServletRequest request = requestFrom("10.0.0.2");
        request.addHeader("X-Forwarded-For", "[2001:db8::1]");
        assertThatThrownBy(() -> resolver(ClientIpProperties.Mode.TRUSTED_PROXY).resolve(request))
                .isInstanceOf(ClientIpResolutionException.class);
    }

    @Test
    void failsClosedForMultiplePhysicalHeaderLinesBeforePrecedence() {
        MockHttpServletRequest forwarded = requestFrom("10.0.0.2");
        forwarded.addHeader("Forwarded", "for=8.8.8.8");
        forwarded.addHeader("Forwarded", "for=1.1.1.1");
        assertThatThrownBy(() -> resolver(ClientIpProperties.Mode.TRUSTED_PROXY).resolve(forwarded))
                .isInstanceOf(ClientIpResolutionException.class);

        MockHttpServletRequest xff = requestFrom("10.0.0.2");
        xff.addHeader("X-Forwarded-For", "8.8.8.8");
        xff.addHeader("X-Forwarded-For", "1.1.1.1");
        assertThatThrownBy(() -> resolver(ClientIpProperties.Mode.TRUSTED_PROXY).resolve(xff))
                .isInstanceOf(ClientIpResolutionException.class);
    }

    @Test
    void forwardedWinsOverConflictingXffAndNeverFallsBackAfterError() {
        MockHttpServletRequest precedence = requestFrom("10.0.0.2");
        precedence.addHeader("Forwarded", "for=8.8.8.8");
        precedence.addHeader("X-Forwarded-For", "1.1.1.1");
        assertThat(resolver(ClientIpProperties.Mode.TRUSTED_PROXY).resolve(precedence).canonicalLiteral())
                .isEqualTo("8.8.8.8");

        MockHttpServletRequest malformed = requestFrom("10.0.0.2");
        malformed.addHeader("Forwarded", "for=unknown");
        malformed.addHeader("X-Forwarded-For", "8.8.8.8");
        assertThatThrownBy(() -> resolver(ClientIpProperties.Mode.TRUSTED_PROXY).resolve(malformed))
                .isInstanceOf(ClientIpResolutionException.class);
    }

    @Test
    void enforcesHeaderAndHopBounds() {
        MockHttpServletRequest longHeader = requestFrom("10.0.0.2");
        longHeader.addHeader("Forwarded", "for=" + "1".repeat(4_100));
        assertThatThrownBy(() -> resolver(ClientIpProperties.Mode.TRUSTED_PROXY).resolve(longHeader))
                .isInstanceOf(ClientIpResolutionException.class);

        MockHttpServletRequest manyHops = requestFrom("10.0.0.2");
        manyHops.addHeader("X-Forwarded-For", String.join(",", java.util.Collections.nCopies(21, "8.8.8.8")));
        assertThatThrownBy(() -> resolver(ClientIpProperties.Mode.TRUSTED_PROXY).resolve(manyHops))
                .isInstanceOf(ClientIpResolutionException.class);
    }

    private ServletClientIpResolver resolver(ClientIpProperties.Mode mode) {
        return new ServletClientIpResolver(new ClientIpProperties(mode, List.of("10.0.0.0/8"), 20, 4096));
    }

    private MockHttpServletRequest requestFrom(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
