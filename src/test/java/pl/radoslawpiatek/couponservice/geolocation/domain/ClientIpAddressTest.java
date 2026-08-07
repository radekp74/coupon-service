package pl.radoslawpiatek.couponservice.geolocation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ClientIpAddressTest {

    @Test
    void acceptsNumericIpv4AndIpv6Literals() {
        assertThat(ClientIpAddress.parseLiteral("8.8.8.8").canonicalLiteral()).isEqualTo("8.8.8.8");
        assertThat(ClientIpAddress.parseLiteral("2001:4860:4860::8888").canonicalLiteral()).contains(":");
    }

    @Test
    void normalizesIpv4MappedIpv6BeforePolicyEvaluation() {
        assertThat(ClientIpAddress.parseLiteral("::ffff:8.8.8.8").canonicalLiteral()).isEqualTo("8.8.8.8");
    }

    @Test
    void rejectsHostNamesZonesPortsWhitespaceAndMalformedLiteralsBeforeDnsCanRun() {
        for (String value : new String[] {
                "example.com",
                "fe80::1%eth0",
                "8.8.8.8:53",
                "[2001:db8::1]",
                "256.1.1.1",
                "01.2.3.4",
                "1.2.3",
                "gggg::1",
                " "
        }) {
            assertThatThrownBy(() -> ClientIpAddress.parseLiteral(value))
                    .isInstanceOf(ClientIpResolutionException.class)
                    .hasMessage("Client address could not be resolved.");
        }
        assertThatThrownBy(() -> ClientIpAddress.parseLiteral(null))
                .isInstanceOf(ClientIpResolutionException.class);
    }

    @Test
    void equalityAndHashCodeUseNormalizedAddressBytes() {
        ClientIpAddress first = ClientIpAddress.parseLiteral("2001:4860:4860::8888");
        ClientIpAddress equivalent = ClientIpAddress.parseLiteral("2001:4860:4860:0:0:0:0:8888");
        ClientIpAddress different = ClientIpAddress.parseLiteral("2001:4860:4860::8844");

        assertThat(first).isEqualTo(first).isEqualTo(equivalent).isNotEqualTo(different).isNotEqualTo("not-an-address");
        assertThat(first.hashCode()).isEqualTo(equivalent.hashCode());
    }
}
