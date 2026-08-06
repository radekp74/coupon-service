package pl.radoslawpiatek.couponservice.geolocation.adapters;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpResolutionException;

/** Parses only the project-defined safe subset of RFC 7239 {@code Forwarded}. */
final class ForwardedHeaderParser {

    List<ClientIpAddress> parse(String header, int maximumLength, int maximumHops) {
        validateHeader(header, maximumLength);
        List<String> elements = splitCommaSeparated(header);
        if (elements.isEmpty() || elements.size() > maximumHops) {
            throw new ClientIpResolutionException();
        }
        List<ClientIpAddress> addresses = new ArrayList<>(elements.size());
        for (String element : elements) {
            addresses.add(parseElement(element));
        }
        return List.copyOf(addresses);
    }

    private ClientIpAddress parseElement(String element) {
        String value = null;
        for (String parameter : element.split(";", -1)) {
            String[] pair = parameter.trim().split("=", 2);
            if (pair.length != 2 || pair[0].isBlank() || pair[1].isBlank()) {
                throw new ClientIpResolutionException();
            }
            if ("for".equals(pair[0].trim().toLowerCase(Locale.ROOT))) {
                if (value != null) {
                    throw new ClientIpResolutionException();
                }
                value = parseForValue(pair[1].trim());
            }
        }
        if (value == null) {
            throw new ClientIpResolutionException();
        }
        return ClientIpAddress.parseLiteral(value);
    }

    private String parseForValue(String rawValue) {
        if (rawValue.indexOf('\\') >= 0 || rawValue.equalsIgnoreCase("unknown") || rawValue.startsWith("_")) {
            throw new ClientIpResolutionException();
        }
        String value = rawValue;
        if (value.startsWith("\"") || value.endsWith("\"")) {
            if (value.length() < 2 || !value.startsWith("\"") || !value.endsWith("\"")) {
                throw new ClientIpResolutionException();
            }
            value = value.substring(1, value.length() - 1);
        }
        if (value.startsWith("[")) {
            int close = value.indexOf(']');
            if (close < 2) {
                throw new ClientIpResolutionException();
            }
            String literal = value.substring(1, close);
            String suffix = value.substring(close + 1);
            if (!suffix.isEmpty()) {
                if (!suffix.startsWith(":") || !validPort(suffix.substring(1))) {
                    throw new ClientIpResolutionException();
                }
            }
            return literal;
        }
        if (value.contains("[") || value.contains("]") || value.contains(":")) {
            throw new ClientIpResolutionException();
        }
        return value;
    }

    private boolean validPort(String port) {
        try {
            int numericPort = Integer.parseInt(port);
            return numericPort >= 1 && numericPort <= 65_535 && String.valueOf(numericPort).equals(port);
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    static List<String> splitCommaSeparated(String header) {
        List<String> values = new ArrayList<>();
        boolean quoted = false;
        int start = 0;
        for (int index = 0; index < header.length(); index++) {
            char character = header.charAt(index);
            if (character == '\\') {
                throw new ClientIpResolutionException();
            }
            if (character == '"') {
                quoted = !quoted;
            } else if (character == ',' && !quoted) {
                addElement(values, header.substring(start, index));
                start = index + 1;
            }
        }
        if (quoted) {
            throw new ClientIpResolutionException();
        }
        addElement(values, header.substring(start));
        return values;
    }

    static void validateHeader(String header, int maximumLength) {
        if (header == null || header.isBlank() || header.length() > maximumLength) {
            throw new ClientIpResolutionException();
        }
    }

    private static void addElement(List<String> values, String value) {
        if (value.isBlank()) {
            throw new ClientIpResolutionException();
        }
        values.add(value.trim());
    }
}
