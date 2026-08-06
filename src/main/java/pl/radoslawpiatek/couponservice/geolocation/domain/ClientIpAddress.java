package pl.radoslawpiatek.couponservice.geolocation.domain;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Verified numeric client address retained only while a request is being processed.
 *
 * <p>The factory accepts a strict IPv4 or IPv6 literal before delegating to the JDK parser,
 * so it never resolves host names. Callers must not persist or log this value.
 */
public final class ClientIpAddress {

    private static final Pattern IPV4 = Pattern.compile("(?:0|[1-9]\\d{0,2})(?:\\.(?:0|[1-9]\\d{0,2})){3}");
    private static final Pattern IPV6_CHARACTERS = Pattern.compile("[0-9A-Fa-f:.]+", Pattern.CASE_INSENSITIVE);

    private final InetAddress inetAddress;

    private ClientIpAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    /**
     * Parses one unbracketed numeric address literal without DNS.
     *
     * @param literal address supplied by a trusted transport or already validated header grammar
     * @return immutable in-memory representation of the literal
     * @throws ClientIpResolutionException when the value is not a strict IPv4 or IPv6 literal
     */
    public static ClientIpAddress parseLiteral(String literal) {
        if (literal == null || literal.isBlank() || literal.contains("%")) {
            throw new ClientIpResolutionException();
        }
        boolean ipv4 = IPV4.matcher(literal).matches() && validIpv4Octets(literal);
        boolean ipv6 = literal.indexOf(':') >= 0 && IPV6_CHARACTERS.matcher(literal).matches();
        if (!ipv4 && !ipv6) {
            throw new ClientIpResolutionException();
        }
        try {
            InetAddress parsed = InetAddress.getByName(literal);
            boolean normalizedIpv4MappedIpv6 = ipv6 && literal.toLowerCase().startsWith("::ffff:")
                    && parsed instanceof Inet4Address;
            if ((ipv4 && !(parsed instanceof Inet4Address))
                    || (ipv6 && !(parsed instanceof Inet6Address) && !normalizedIpv4MappedIpv6)) {
                throw new ClientIpResolutionException();
            }
            return new ClientIpAddress(parsed);
        } catch (UnknownHostException exception) {
            throw new ClientIpResolutionException();
        }
    }

    /**
     * Returns the parsed address for byte-level CIDR matching and a safe HTTP URI segment.
     *
     * <p>The returned object is not an authorization claim and must not be written to logs or storage.
     *
     * @return numeric address represented by this value
     */
    public InetAddress inetAddress() {
        return inetAddress;
    }

    /**
     * Returns the canonical numeric text only for a request to the configured GeoIP provider.
     *
     * @return normalized literal with no port, hostname, zone identifier or user-supplied syntax
     */
    public String canonicalLiteral() {
        return inetAddress.getHostAddress();
    }

    private static boolean validIpv4Octets(String literal) {
        for (String part : literal.split("\\.", -1)) {
            try {
                if (Integer.parseInt(part) > 255) {
                    return false;
                }
            } catch (NumberFormatException exception) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ClientIpAddress address
                && Arrays.equals(inetAddress.getAddress(), address.inetAddress.getAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(inetAddress.getAddress()));
    }
}
