package pl.radoslawpiatek.couponservice.coupon.adapters.persistence;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.postgresql.util.PSQLException;
import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponAlreadyRedeemedException;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponCode;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryCode;
import pl.radoslawpiatek.couponservice.coupon.domain.UserId;
import pl.radoslawpiatek.couponservice.coupon.ports.CouponRedemptionRepository;

/** PostgreSQL adapter for redemption snapshots, locks and the atomic counter write. */
@Repository
public class JdbcCouponRedemptionRepository implements CouponRedemptionRepository {
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String USER_CONSTRAINT = "uq_coupon_redemptions_coupon_user";
    private final JdbcClient jdbc;

    /** @param jdbc parameterized PostgreSQL client */
    public JdbcCouponRedemptionRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<Coupon> findSnapshot(String normalizedCode) { return find(normalizedCode, false); }

    @Override
    public Optional<Coupon> findForUpdate(String normalizedCode) { return find(normalizedCode, true); }

    private Optional<Coupon> find(String normalizedCode, boolean locked) {
        String sql = "SELECT id, code, created_at, max_uses, current_uses, country_code FROM coupons "
                + "WHERE normalized_code = :code" + (locked ? " FOR UPDATE" : "");
        return jdbc.sql(sql).param("code", normalizedCode).query((rs, rowNum) -> new Coupon(
                rs.getObject("id", UUID.class), CouponCode.of(rs.getString("code")),
                rs.getObject("created_at", OffsetDateTime.class), rs.getInt("max_uses"),
                rs.getInt("current_uses"), CountryCode.of(rs.getString("country_code")))).optional();
    }

    @Override
    public boolean exists(UUID couponId, UserId userId) {
        Long count = jdbc.sql("SELECT COUNT(*) FROM coupon_redemptions WHERE coupon_id=:couponId AND user_id=:userId")
                .param("couponId", couponId).param("userId", userId.value()).query(Long.class).single();
        return count > 0;
    }

    @Override
    public void insert(UUID redemptionId, UUID couponId, UserId userId, String countryCode, OffsetDateTime redeemedAt) {
        try {
            jdbc.sql("""
                    INSERT INTO coupon_redemptions (id, coupon_id, user_id, resolved_country_code, redeemed_at)
                    VALUES (:id, :couponId, :userId, :countryCode, :redeemedAt)
                    """)
                    .param("id", redemptionId).param("couponId", couponId).param("userId", userId.value())
                    .param("countryCode", countryCode).param("redeemedAt", redeemedAt).update();
        } catch (DataIntegrityViolationException exception) {
            if (hasNamedConstraint(exception, USER_CONSTRAINT)) throw new CouponAlreadyRedeemedException();
            throw exception;
        }
    }

    @Override
    public Optional<Integer> incrementIfCapacity(UUID couponId) {
        return jdbc.sql("""
                UPDATE coupons SET current_uses=current_uses + 1
                WHERE id=:id AND current_uses < max_uses
                RETURNING current_uses
                """)
                .param("id", couponId).query(Integer.class).optional();
    }

    private boolean hasNamedConstraint(Throwable failure, String expected) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof PSQLException sql && expected.equals(sql.getServerErrorMessage() == null
                    ? null : sql.getServerErrorMessage().getConstraint())) return true;
        }
        return false;
    }
}
