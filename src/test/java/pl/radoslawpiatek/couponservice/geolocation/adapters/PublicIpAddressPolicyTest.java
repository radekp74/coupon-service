package pl.radoslawpiatek.couponservice.geolocation.adapters;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;

class PublicIpAddressPolicyTest {

    private final PublicIpAddressPolicy policy = new PublicIpAddressPolicy();

    @ParameterizedTest
    @ValueSource(strings = {
            "0.0.0.0", "127.0.0.1", "10.1.2.3", "100.64.0.1", "169.254.1.1", "172.16.1.1",
            "192.0.2.1", "192.168.1.1", "198.18.1.1", "198.51.100.1", "203.0.113.1", "224.0.0.1",
            "::", "::1", "100::1", "2001:db8::1", "fc00::1", "fe80::1", "ff02::1", "::ffff:192.0.2.1"
    })
    void blocksPrivateSpecialPurposeAndDocumentationAddresses(String literal) {
        assertThat(policy.permitsPublicLookup(ClientIpAddress.parseLiteral(literal))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "1.1.1.1", "2606:4700:4700::1111"})
    void permitsPublicUnicastAddresses(String literal) {
        assertThat(policy.permitsPublicLookup(ClientIpAddress.parseLiteral(literal))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0/0", "8.8.8.8/32", "::/0", "2001:4860:4860::8888/128"})
    void supportsFamilyCorrectCidrBoundaries(String cidr) {
        CidrBlock.parse(cidr);
    }
}
