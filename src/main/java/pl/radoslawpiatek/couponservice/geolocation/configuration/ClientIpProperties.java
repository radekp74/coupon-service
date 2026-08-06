package pl.radoslawpiatek.couponservice.geolocation.configuration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import pl.radoslawpiatek.couponservice.geolocation.adapters.CidrBlock;

/**
 * Configuration defining whether servlet forwarding headers form part of the trust boundary.
 *
 * <p>{@code DIRECT} ignores all forwarded headers. {@code TRUSTED_PROXY} is valid only with
 * configured numeric CIDRs and imposes bounded parsing of each single physical header field-line.
 *
 * @param mode trust-boundary mode; direct is the safe default
 * @param trustedProxies numeric CIDRs allowed to be immediate transport peers in trusted mode
 * @param maxForwardedHops maximum number of comma-delimited proxy hops accepted from one header
 * @param maxHeaderLength maximum character count accepted from one forwarding header field-line
 */
@Validated
@ConfigurationProperties("coupon.client-ip")
public record ClientIpProperties(
        @NotNull Mode mode,
        List<String> trustedProxies,
        @Min(1) @Max(20) int maxForwardedHops,
        @Min(1) @Max(4096) int maxHeaderLength
) {
    /** Supported trust modes; unknown configuration values fail binding at startup. */
    public enum Mode {
        /** Uses only the servlet transport peer. */
        DIRECT,
        /** Uses a bounded proxy chain only after trusting the immediate transport peer. */
        TRUSTED_PROXY
    }

    /**
     * Applies defaults and validates cross-field security invariants.
     *
     * @throws IllegalArgumentException when trusted-proxy mode lacks an explicit CIDR boundary
     */
    public ClientIpProperties {
        trustedProxies = trustedProxies == null ? List.of() : List.copyOf(trustedProxies);
        if (mode == Mode.TRUSTED_PROXY && trustedProxies.isEmpty()) {
            throw new IllegalArgumentException("Trusted proxy mode requires at least one CIDR.");
        }
        trustedProxies.forEach(CidrBlock::parse);
    }
}
