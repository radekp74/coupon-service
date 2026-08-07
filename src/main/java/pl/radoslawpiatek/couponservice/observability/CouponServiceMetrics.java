package pl.radoslawpiatek.couponservice.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Low-cardinality metrics facade for coupon business outcomes and infrastructure boundaries.
 *
 * <p>The enums are the complete tag vocabularies. Request identifiers, coupon codes, user
 * identifiers, IP addresses, countries, URLs and exception text are intentionally absent from
 * this API so they cannot accidentally become metric labels.
 */
@Component
public final class CouponServiceMetrics {

    /** Coupon creation terminal outcomes. */
    public enum CreateOutcome {
        /** Coupon was persisted. */ SUCCESS("success"),
        /** Canonical coupon code already existed. */ CONFLICT("conflict");
        private final String tag;
        CreateOutcome(String tag) { this.tag = tag; }
        String tag() { return tag; }
    }

    /** Coupon redemption terminal outcomes exposed by the application boundary. */
    public enum RedemptionOutcome {
        /** Redemption committed. */ SUCCESS("success"),
        /** Coupon snapshot was absent. */ NOT_FOUND("not_found"),
        /** Resolved country did not match the coupon. */ COUNTRY_NOT_ALLOWED("country_not_allowed"),
        /** User had already redeemed the coupon. */ ALREADY_REDEEMED("already_redeemed"),
        /** Coupon capacity was exhausted. */ EXHAUSTED("exhausted"),
        /** Client-IP or GeoIP infrastructure was unavailable. */ GEOLOCATION_UNAVAILABLE("geolocation_unavailable"),
        /** Unexpected application failure. */ INTERNAL_ERROR("internal_error");
        private final String tag;
        RedemptionOutcome(String tag) { this.tag = tag; }
        String tag() { return tag; }
    }

    /** Source used to resolve the client address. */
    public enum ClientIpSource {
        /** Direct servlet peer. */ DIRECT("direct"),
        /** RFC Forwarded header chain. */ FORWARDED("forwarded"),
        /** X-Forwarded-For header chain. */ X_FORWARDED_FOR("x_forwarded_for");
        private final String tag;
        ClientIpSource(String tag) { this.tag = tag; }
        String tag() { return tag; }
    }

    /** Client-address resolution outcome. */
    public enum ClientIpOutcome {
        /** Resolution succeeded. */ SUCCESS("success"),
        /** Resolution failed closed. */ FAILURE("failure");
        private final String tag;
        ClientIpOutcome(String tag) { this.tag = tag; }
        String tag() { return tag; }
    }

    /** GeoIP provider identifier. */
    public enum GeoProvider {
        /** ipwho.is HTTPS adapter. */ IPWHOIS("ipwhois"),
        /** Deterministic local/test stub. */ STUB("stub");
        private final String tag;
        GeoProvider(String tag) { this.tag = tag; }
        String tag() { return tag; }
    }

    /** GeoIP outcome vocabulary shared by the counter and provider timer. */
    public enum GeolocationOutcome {
        /** Provider returned a valid country. */ SUCCESS("success"),
        /** Provider request timed out. */ TIMEOUT("timeout"),
        /** Provider returned HTTP 429. */ RATE_LIMITED("rate_limited"),
        /** Provider or transport failed without a valid country. */ PROVIDER_ERROR("provider_error"),
        /** Provider response violated the bounded JSON contract. */ INVALID_RESPONSE("invalid_response"),
        /** Address policy blocked provider egress. */ NON_PUBLIC_IP("non_public_ip");
        private final String tag;
        GeolocationOutcome(String tag) { this.tag = tag; }
        String tag() { return tag; }
    }

    /** Short database transaction outcomes. */
    public enum TransactionOutcome {
        /** Transaction committed. */ SUCCESS("success"),
        /** Coupon was absent under lock. */ NOT_FOUND("not_found"),
        /** Locked coupon country no longer matched. */ COUNTRY_NOT_ALLOWED("country_not_allowed"),
        /** User was already redeemed under lock. */ ALREADY_REDEEMED("already_redeemed"),
        /** Capacity was exhausted under lock. */ EXHAUSTED("exhausted"),
        /** Unexpected persistence/transaction failure. */ DATABASE_ERROR("database_error");
        private final String tag;
        TransactionOutcome(String tag) { this.tag = tag; }
        String tag() { return tag; }
    }

    private final MeterRegistry registry;
    private final Map<CreateOutcome, Counter> createCounters = new EnumMap<>(CreateOutcome.class);
    private final Map<RedemptionOutcome, Counter> redemptionCounters = new EnumMap<>(RedemptionOutcome.class);

    /**
     * Creates the facade and pre-registers bounded counter vocabularies.
     *
     * @param registry application meter registry managed by Spring Boot
     */
    public CouponServiceMetrics(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry);
        for (CreateOutcome outcome : CreateOutcome.values()) {
            createCounters.put(outcome, Counter.builder("coupon.create")
                    .tag("outcome", outcome.tag()).register(registry));
        }
        for (RedemptionOutcome outcome : RedemptionOutcome.values()) {
            redemptionCounters.put(outcome, Counter.builder("coupon.redemption")
                    .tag("outcome", outcome.tag()).register(registry));
        }
        for (ClientIpSource source : ClientIpSource.values()) {
            for (ClientIpOutcome outcome : ClientIpOutcome.values()) {
                clientIpCounter(source, outcome);
            }
        }
        for (GeoProvider provider : GeoProvider.values()) {
            for (GeolocationOutcome outcome : GeolocationOutcome.values()) {
                geolocationCounter(provider, outcome);
                geolocationTimer(provider, outcome);
            }
        }
        for (TransactionOutcome outcome : TransactionOutcome.values()) {
            transactionTimer(outcome);
        }
    }

    /** Records one completed coupon-create business outcome.
     * @param outcome bounded terminal create outcome
     */
    public void recordCreate(CreateOutcome outcome) {
        createCounters.get(Objects.requireNonNull(outcome)).increment();
    }

    /** Records one completed coupon-redemption application outcome.
     * @param outcome bounded terminal redemption outcome
     */
    public void recordRedemption(RedemptionOutcome outcome) {
        redemptionCounters.get(Objects.requireNonNull(outcome)).increment();
    }

    /** Records one client-address resolution attempt.
     * @param source bounded address source
     * @param outcome success or fail-closed result
     */
    public void recordClientIp(ClientIpSource source, ClientIpOutcome outcome) {
        clientIpCounter(source, outcome).increment();
    }

    /** Records one GeoIP resolution outcome.
     * @param provider bounded provider identifier
     * @param outcome bounded provider outcome
     */
    public void recordGeolocation(GeoProvider provider, GeolocationOutcome outcome) {
        geolocationCounter(provider, outcome).increment();
    }

    /** Starts a provider timer without making any provider call itself.
     * @return timer sample to stop against the terminal provider outcome
     */
    public Timer.Sample startGeolocationTimer() {
        return Timer.start(registry);
    }

    /** Stops the provider timer against the bounded provider/outcome tag pair.
     * @param sample previously started timer sample
     * @param provider bounded provider identifier
     * @param outcome bounded provider outcome
     */
    public void stopGeolocationTimer(Timer.Sample sample, GeoProvider provider, GeolocationOutcome outcome) {
        Objects.requireNonNull(sample).stop(geolocationTimer(provider, outcome));
    }

    /** Starts timing the short database transaction only.
     * @return timer sample scoped to the proxied database transaction
     */
    public Timer.Sample startTransactionTimer() {
        return Timer.start(registry);
    }

    /** Stops the short transaction timer with its terminal low-cardinality outcome.
     * @param sample previously started transaction timer sample
     * @param outcome bounded terminal transaction outcome
     */
    public void stopTransactionTimer(Timer.Sample sample, TransactionOutcome outcome) {
        Objects.requireNonNull(sample).stop(transactionTimer(outcome));
    }

    private Counter clientIpCounter(ClientIpSource source, ClientIpOutcome outcome) {
        return Counter.builder("client.ip.resolution")
                .tag("source", Objects.requireNonNull(source).tag())
                .tag("outcome", Objects.requireNonNull(outcome).tag())
                .register(registry);
    }

    private Counter geolocationCounter(GeoProvider provider, GeolocationOutcome outcome) {
        return Counter.builder("geolocation.resolution")
                .tag("provider", Objects.requireNonNull(provider).tag())
                .tag("outcome", Objects.requireNonNull(outcome).tag())
                .register(registry);
    }

    private Timer geolocationTimer(GeoProvider provider, GeolocationOutcome outcome) {
        return Timer.builder("geolocation.provider")
                .tag("provider", Objects.requireNonNull(provider).tag())
                .tag("outcome", Objects.requireNonNull(outcome).tag())
                .register(registry);
    }

    private Timer transactionTimer(TransactionOutcome outcome) {
        return Timer.builder("coupon.redemption.transaction")
                .tag("outcome", Objects.requireNonNull(outcome).tag())
                .register(registry);
    }
}
