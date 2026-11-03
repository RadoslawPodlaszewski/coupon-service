package com.example.coupons.infrastructure.persistence;

import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.coupons.application.query.CouponReadModel;
import com.example.coupons.application.query.CouponView;

/**
 * Query-side adapter: plain SQL straight into a read model. It does not load the aggregate and takes no locks,
 * so reads never contend with redemptions. Today it reads the same tables as the write side; it could be
 * pointed at a replica, a materialized view or a separate store without touching the rest of the code.
 */
@Repository
class JdbcCouponReadModel implements CouponReadModel {

    private final JdbcClient jdbc;

    JdbcCouponReadModel(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CouponView> findByCode(String normalizedCode) {
        return jdbc.sql("""
                        SELECT code, created_at, max_usages, usage_count,
                               max_usages - usage_count AS remaining_usages,
                               country_code
                          FROM coupons
                         WHERE code = :code
                        """)
                .param("code", normalizedCode)
                .query((rs, rowNum) -> new CouponView(
                        rs.getString("code"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getInt("max_usages"),
                        rs.getInt("usage_count"),
                        rs.getInt("remaining_usages"),
                        rs.getString("country_code")))
                .optional();
    }
}
