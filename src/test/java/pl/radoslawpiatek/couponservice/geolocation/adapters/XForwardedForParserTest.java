package pl.radoslawpiatek.couponservice.geolocation.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpResolutionException;

class XForwardedForParserTest {

    private final XForwardedForParser parser = new XForwardedForParser();

    @Test
    void acceptsOnlyUnbracketedIpv4AndIpv6Literals() {
        assertThat(parser.parse("8.8.8.8, 1.1.1.1", 4096, 20))
                .extracting(address -> address.canonicalLiteral())
                .containsExactly("8.8.8.8", "1.1.1.1");
        assertThat(parser.parse("2001:4860:4860::8888", 4096, 20).getFirst().canonicalLiteral())
                .contains(":");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[2001:4860:4860::8888]",
            "8.8.8.8:443",
            "example.com",
            "fe80::1%eth0",
            "unknown"
    })
    void rejectsPortsBracketsHostnamesZonesAndSentinels(String header) {
        assertThatThrownBy(() -> parser.parse(header, 4096, 20))
                .isInstanceOf(ClientIpResolutionException.class);
    }

    @Test
    void rejectsEmptyElementsAndTooManyHops() {
        assertThatThrownBy(() -> parser.parse("8.8.8.8,,1.1.1.1", 4096, 20))
                .isInstanceOf(ClientIpResolutionException.class);
        assertThatThrownBy(() -> parser.parse(String.join(",", java.util.Collections.nCopies(21, "8.8.8.8")), 4096, 20))
                .isInstanceOf(ClientIpResolutionException.class);
    }
}
