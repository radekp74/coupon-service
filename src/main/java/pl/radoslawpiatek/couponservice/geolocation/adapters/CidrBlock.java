package pl.radoslawpiatek.couponservice.geolocation.adapters;

import java.util.Arrays;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;

/**
 * Immutable numeric CIDR range used exclusively for configured proxy and address-policy matching.
 *
 * <p>It parses only numeric literals through {@link ClientIpAddress}, so configuration validation
 * cannot resolve host names.
 */
public final class CidrBlock {

    private final byte[] network;
    private final int prefixLength;

    private CidrBlock(byte[] network, int prefixLength) {
        this.network = network;
        this.prefixLength = prefixLength;
    }

    /**
     * Parses a configured IPv4 or IPv6 CIDR.
     *
     * @param value numeric network followed by a family-valid prefix length
     * @return immutable CIDR matcher
     * @throws IllegalArgumentException when the network or prefix is invalid
     */
    public static CidrBlock parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("CIDR must be present.");
        }
        String[] parts = value.split("/", -1);
        if (parts.length != 2 || parts[1].isEmpty()) {
            throw new IllegalArgumentException("CIDR must have one prefix length.");
        }
        ClientIpAddress address = ClientIpAddress.parseLiteral(parts[0]);
        try {
            int prefix = Integer.parseInt(parts[1]);
            int maximum = address.inetAddress().getAddress().length * Byte.SIZE;
            if (prefix < 0 || prefix > maximum) {
                throw new IllegalArgumentException("CIDR prefix is outside its address family.");
            }
            return new CidrBlock(address.inetAddress().getAddress(), prefix);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("CIDR prefix must be numeric.");
        }
    }

    /**
     * Tests an address using a deterministic byte-prefix comparison without name resolution.
     *
     * @param address verified numeric address to check
     * @return whether the address belongs to this network and address family
     */
    public boolean contains(ClientIpAddress address) {
        byte[] candidate = address.inetAddress().getAddress();
        if (candidate.length != network.length) {
            return false;
        }
        int completeBytes = prefixLength / Byte.SIZE;
        if (!Arrays.equals(Arrays.copyOf(network, completeBytes), Arrays.copyOf(candidate, completeBytes))) {
            return false;
        }
        int remainder = prefixLength % Byte.SIZE;
        if (remainder == 0) {
            return true;
        }
        int mask = 0xff << (Byte.SIZE - remainder);
        return (network[completeBytes] & mask) == (candidate[completeBytes] & mask);
    }
}
