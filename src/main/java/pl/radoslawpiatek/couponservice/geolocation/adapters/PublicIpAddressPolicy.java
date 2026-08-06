package pl.radoslawpiatek.couponservice.geolocation.adapters;

import java.util.List;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;

/**
 * Auditable deny-list of IANA special-purpose networks that must never reach a public GeoIP provider.
 *
 * <p>It intentionally does not rely on the incomplete {@code InetAddress} convenience predicates.
 * Both address families are compared as bytes through CIDR ranges, and an IPv4-mapped IPv6 literal
 * is normalized by {@link ClientIpAddress} before this policy is evaluated.
 */
public final class PublicIpAddressPolicy {

    private static final List<CidrBlock> NON_PUBLIC_RANGES = List.of(
            CidrBlock.parse("0.0.0.0/8"), CidrBlock.parse("10.0.0.0/8"),
            CidrBlock.parse("100.64.0.0/10"), CidrBlock.parse("127.0.0.0/8"),
            CidrBlock.parse("169.254.0.0/16"), CidrBlock.parse("172.16.0.0/12"),
            CidrBlock.parse("192.0.0.0/24"), CidrBlock.parse("192.0.2.0/24"),
            CidrBlock.parse("192.168.0.0/16"), CidrBlock.parse("198.18.0.0/15"),
            CidrBlock.parse("198.51.100.0/24"), CidrBlock.parse("203.0.113.0/24"),
            CidrBlock.parse("224.0.0.0/4"), CidrBlock.parse("240.0.0.0/4"),
            CidrBlock.parse("::/128"), CidrBlock.parse("::1/128"),
            CidrBlock.parse("100::/64"), CidrBlock.parse("2001:db8::/32"),
            CidrBlock.parse("fc00::/7"), CidrBlock.parse("fe80::/10"), CidrBlock.parse("ff00::/8")
    );

    /** Creates the stateless policy backed by the immutable IANA-derived CIDR table. */
    public PublicIpAddressPolicy() {
    }

    /**
     * Determines whether public egress is permitted for an already verified address.
     *
     * @param address memory-only address under consideration for provider egress
     * @return {@code true} only for globally routable unicast addresses outside the documented ranges
     */
    public boolean permitsPublicLookup(ClientIpAddress address) {
        return NON_PUBLIC_RANGES.stream().noneMatch(range -> range.contains(address));
    }
}
