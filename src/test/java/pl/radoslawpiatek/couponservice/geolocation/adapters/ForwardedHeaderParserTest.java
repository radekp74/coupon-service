package pl.radoslawpiatek.couponservice.geolocation.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpResolutionException;

class ForwardedHeaderParserTest {

    private final ForwardedHeaderParser parser = new ForwardedHeaderParser();

    @Test
    void acceptsOnlyTheFrozenIpv4AndBracketedIpv6Forms() {
        assertThat(literals(parser.parse("for=8.8.8.8", 4096, 20)))
                .containsExactly("8.8.8.8");

        String ipv6WithoutPort = parser.parse("for=\"[2001:4860:4860::8888]\"", 4096, 20)
                .getFirst().canonicalLiteral();
        String ipv6WithPort = parser.parse("for=\"[2001:4860:4860::8888]:4711\"", 4096, 20)
                .getFirst().canonicalLiteral();

        assertThat(ipv6WithoutPort).contains(":");
        assertThat(ipv6WithPort).isEqualTo(ipv6WithoutPort);
    }

    @Test
    void acceptsACommaSeparatedChainAndIgnoresNonForParameters() {
        assertThat(literals(parser.parse("for=8.8.8.8;proto=https, for=1.1.1.1;by=10.0.0.1", 4096, 20)))
                .containsExactly("8.8.8.8", "1.1.1.1");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "for",
            "for=",
            "=8.8.8.8",
            "by=10.0.0.1",
            "for=8.8.8.8;for=1.1.1.1",
            "for=unknown",
            "for=_hidden",
            "for=example.com",
            "for=8.8.8.8:443",
            "for=\"[2001:db8::1]:0\"",
            "for=\"[2001:db8::1]:65536\"",
            "for=\"[2001:db8::1]:01\"",
            "for=\"[2001:db8::1]:abc\"",
            "for=\"[2001:db8::1]:443extra\"",
            "for=\"[]\"",
            "for=\"[2001:db8::1\"",
            "for=\"[fe80::1%eth0]\"",
            "for=\"8.8.8.8",
            "for=8.8.8.8\""
    })
    void rejectsMalformedOrAmbiguousForwardedForms(String header) {
        assertThatThrownBy(() -> parser.parse(header, 4096, 20))
                .isInstanceOf(ClientIpResolutionException.class);
    }

    @Test
    void rejectsEscapesEmptyElementsAndUnbalancedQuotes() {
        for (String header : List.of(
                "for=8.8.8.8\\",
                "for=8.8.8.8,,for=1.1.1.1",
                ",for=8.8.8.8",
                "for=8.8.8.8,",
                "for=\"8.8.8.8, for=1.1.1.1"
        )) {
            assertThatThrownBy(() -> parser.parse(header, 4096, 20))
                    .isInstanceOf(ClientIpResolutionException.class);
        }
    }

    @Test
    void enforcesNullBlankLengthAndHopBounds() {
        assertThatThrownBy(() -> parser.parse(null, 4096, 20))
                .isInstanceOf(ClientIpResolutionException.class);
        assertThatThrownBy(() -> parser.parse(" ", 4096, 20))
                .isInstanceOf(ClientIpResolutionException.class);
        assertThatThrownBy(() -> parser.parse("for=" + "1".repeat(4097), 4096, 20))
                .isInstanceOf(ClientIpResolutionException.class);
        assertThatThrownBy(() -> parser.parse(String.join(",", java.util.Collections.nCopies(21, "for=8.8.8.8")), 4096, 20))
                .isInstanceOf(ClientIpResolutionException.class);
    }

    private List<String> literals(List<ClientIpAddress> addresses) {
        return addresses.stream().map(ClientIpAddress::canonicalLiteral).toList();
    }
}
