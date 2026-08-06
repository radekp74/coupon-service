package pl.radoslawpiatek.couponservice.coupon.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/** Validated ISO 3166-1 alpha-2 country code stored in uppercase form. */
public final class CountryCode {

    private static final Set<String> ISO_ALPHA_2 = Set.copyOf(Arrays.asList(Locale.getISOCountries()));

    private final String value;

    private CountryCode(String value) {
        this.value = value;
    }

    /**
     * Normalizes and validates a country code against the JDK ISO country list.
     *
     * @param rawValue country code supplied by an API client
     * @return a validated uppercase ISO 3166-1 alpha-2 code
     * @throws InvalidCouponValueException when the value is null or not a known country code
     */
    public static CountryCode of(String rawValue) {
        if (rawValue == null) {
            throw new InvalidCouponValueException("countryCode", "Country code is required.");
        }

        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        if (!ISO_ALPHA_2.contains(normalized)) {
            throw new InvalidCouponValueException(
                    "countryCode",
                    "Country code must be a valid ISO 3166-1 alpha-2 value."
            );
        }
        return new CountryCode(normalized);
    }

    /**
     * Returns the persisted canonical representation.
     *
     * @return the normalized uppercase country code
     */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof CountryCode countryCode && value.equals(countryCode.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
