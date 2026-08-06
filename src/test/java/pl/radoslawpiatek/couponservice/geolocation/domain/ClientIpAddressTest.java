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
    void rejectsHostNamesZonesAndPortsBeforeDnsCanRun() {
        for (String value : new String[] {"example.com", "fe80::1%eth0", "8.8.8.8:53", "[2001:db8::1]"}) {
            assertThatThrownBy(() -> ClientIpAddress.parseLiteral(value))
                    .isInstanceOf(ClientIpResolutionException.class)
                    .hasMessage("Client address could not be resolved.");
        }
    }
}
