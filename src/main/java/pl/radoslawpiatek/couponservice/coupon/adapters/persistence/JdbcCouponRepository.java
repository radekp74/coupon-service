package pl.radoslawpiatek.couponservice.coupon.adapters.persistence;

import java.sql.SQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.radoslawpiatek.couponservice.coupon.domain.Coupon;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponCodeConflictException;
import pl.radoslawpiatek.couponservice.coupon.ports.CouponRepository;

/**
 * PostgreSQL adapter that persists coupons with parameterized SQL.
 *
 * <p>SQLSTATE 23505 is translated to the domain conflict while every other
 * integrity failure remains an unexpected persistence error.
 */
@Repository
public class JdbcCouponRepository implements CouponRepository {

    private static final String POSTGRES_UNIQUE_VIOLATION = "23505";
    private static final String INSERT_SQL = """
            INSERT INTO coupons (
                id,
                code,
                normalized_code,
                created_at,
                max_uses,
                current_uses,
                country_code
            ) VALUES (
                :id,
                :code,
                :normalizedCode,
                :createdAt,
                :maxUses,
                :currentUses,
                :countryCode
            )
            """;

    private final JdbcClient jdbcClient;

    /**
     * Creates the adapter with the JDBC client used for the single write operation.
     *
     * @param jdbcClient parameterized SQL client backed by the application datasource
     */
    public JdbcCouponRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void insert(Coupon coupon) {
        try {
            jdbcClient.sql(INSERT_SQL)
                    .param("id", coupon.id())
                    .param("code", coupon.code().value())
                    .param("normalizedCode", coupon.code().normalizedValue())
                    .param("createdAt", coupon.createdAt())
                    .param("maxUses", coupon.maxUses())
                    .param("currentUses", coupon.currentUses())
                    .param("countryCode", coupon.countryCode().value())
                    .update();
        } catch (DataIntegrityViolationException exception) {
            if (hasSqlState(exception, POSTGRES_UNIQUE_VIOLATION)) {
                throw new CouponCodeConflictException(coupon.code().normalizedValue(), exception);
            }
            throw exception;
        }
    }

    private boolean hasSqlState(Throwable failure, String expectedSqlState) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLException sqlException
                    && expectedSqlState.equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
