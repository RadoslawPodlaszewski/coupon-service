package com.example.coupons.application.query;

import java.util.Optional;

/** Query-side port, implemented by a projection that reads the database directly (no aggregates, no locks). */
public interface CouponReadModel {

    Optional<CouponView> findByCode(String normalizedCode);
}
