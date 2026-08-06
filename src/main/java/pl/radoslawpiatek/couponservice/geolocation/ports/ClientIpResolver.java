package pl.radoslawpiatek.couponservice.geolocation.ports;

import jakarta.servlet.http.HttpServletRequest;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpAddress;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpResolutionException;

/**
 * Resolves the one client address that the deployment trust boundary permits application code to use.
 *
 * <p>Implementations must treat request headers as untrusted unless the immediate transport peer is
 * configured as a trusted proxy. Resolution is memory-only and must never trigger DNS.
 */
public interface ClientIpResolver {

    /**
     * Resolves a verified numeric client address for one servlet request.
     *
     * @param request servlet request whose remote peer and physical header values are inspected
     * @return a verified numeric address without a port or zone identifier
     * @throws ClientIpResolutionException when the transport or trusted-header contract is invalid
     */
    ClientIpAddress resolve(HttpServletRequest request);
}
