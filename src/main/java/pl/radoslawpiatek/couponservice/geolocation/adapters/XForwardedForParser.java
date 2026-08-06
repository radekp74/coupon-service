package pl.radoslawpiatek.couponservice.geolocation.adapters;

import java.util.ArrayList;
import java.util.List;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpResolutionException;

/** Parses comma-separated X-Forwarded-For literals without accepting ports or bracket syntax. */
final class XForwardedForParser {

    List<ClientIpAddress> parse(String header, int maximumLength, int maximumHops) {
        ForwardedHeaderParser.validateHeader(header, maximumLength);
        List<String> values = ForwardedHeaderParser.splitCommaSeparated(header);
        if (values.isEmpty() || values.size() > maximumHops) {
            throw new ClientIpResolutionException();
        }
        List<ClientIpAddress> addresses = new ArrayList<>(values.size());
        for (String value : values) {
            if (value.contains("[") || value.contains("]")) {
                throw new ClientIpResolutionException();
            }
            addresses.add(ClientIpAddress.parseLiteral(value));
        }
        return List.copyOf(addresses);
    }
}
