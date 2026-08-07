package pl.radoslawpiatek.couponservice.geolocation.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpResolutionException;

class CidrBlockTest {

    @Test
    void matchesIpv4BoundariesIncludingPartialBytePrefixes() {
        assertThat(CidrBlock.parse("0.0.0.0/0").contains(ip("203.0.113.9"))).isTrue();
        assertThat(CidrBlock.parse("10.0.0.7/32").contains(ip("10.0.0.7"))).isTrue();
        assertThat(CidrBlock.parse("10.0.0.7/32").contains(ip("10.0.0.8"))).isFalse();
        assertThat(CidrBlock.parse("10.0.0.0/9").contains(ip("10.127.255.255"))).isTrue();
        assertThat(CidrBlock.parse("10.0.0.0/9").contains(ip("10.128.0.1"))).isFalse();
    }

    @Test
    void matchesIpv6BoundariesAndNeverMixesAddressFamilies() {
        CidrBlock allIpv6 = CidrBlock.parse("::/0");
        assertThat(allIpv6.contains(ip("2001:4860:4860::8888"))).isTrue();
        assertThat(allIpv6.contains(ip("8.8.8.8"))).isFalse();

        CidrBlock exact = CidrBlock.parse("2001:4860:4860::8888/128");
        assertThat(exact.contains(ip("2001:4860:4860::8888"))).isTrue();
        assertThat(exact.contains(ip("2001:4860:4860::8844"))).isFalse();
    }

    @Test
    void rejectsMalformedOrOutOfFamilyPrefixes() {
        assertThatThrownBy(() -> CidrBlock.parse(null)).isInstanceOf(IllegalArgumentException.class);
        for (String value : new String[] {
                "10.0.0.0",
                "10.0.0.0/",
                "10.0.0.0/8/extra",
                "10.0.0.0/not-a-number",
                "10.0.0.0/-1",
                "10.0.0.0/33",
                "2001:db8::/129"
        }) {
            assertThatThrownBy(() -> CidrBlock.parse(value))
                    .isInstanceOfAny(IllegalArgumentException.class, ClientIpResolutionException.class);
        }
    }

    private ClientIpAddress ip(String value) {
        return ClientIpAddress.parseLiteral(value);
    }
}
