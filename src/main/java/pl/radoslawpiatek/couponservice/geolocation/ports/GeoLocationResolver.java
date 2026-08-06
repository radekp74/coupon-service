package pl.radoslawpiatek.couponservice.geolocation.ports;

import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;
import pl.radoslawpiatek.couponservice.geolocation.domain.GeolocationUnavailableException;

/**
 * Maps a verified client address to a country without coupling callers to a provider.
 *
 * <p>The address is used only during the call. Implementations must reject non-public addresses
 * before public egress and report all infrastructure failures uniformly.
 */
public interface GeoLocationResolver {

    /**
     * Resolves the country for a verified address.
     *
     * @param clientIpAddress memory-only address produced by {@link ClientIpResolver}
     * @return validated ISO alpha-2 country code
     * @throws GeolocationUnavailableException when policy or provider infrastructure prevents resolution
     */
    CountryCode resolve(ClientIpAddress clientIpAddress);
}
