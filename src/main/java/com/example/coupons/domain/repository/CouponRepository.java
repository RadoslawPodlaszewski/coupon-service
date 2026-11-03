package com.example.coupons.domain.repository;

import java.util.Optional;

import com.example.coupons.domain.exception.DuplicateCouponCodeException;
import com.example.coupons.domain.model.Coupon;
import com.example.coupons.domain.model.CouponCode;

/** Repository port of the Coupon aggregate; implemented in the infrastructure layer. */
public interface CouponRepository {

    /**
     * Stores a new coupon.
     *
     * @throws DuplicateCouponCodeException if a coupon with this (normalized) code already exists
     */
    void add(Coupon coupon);

    /** Persists changes of an already stored coupon. Must be called inside the transaction that loaded it. */
    void update(Coupon coupon);

    Optional<Coupon> findByCode(CouponCode code);

    boolean existsByCode(CouponCode code);

    /**
     * Loads the coupon and holds a pessimistic write lock until the surrounding transaction ends.
     * Redemptions of one coupon are therefore serialized (also across application instances),
     * which lets the aggregate enforce its invariants with plain in-memory logic.
     * Requires an active transaction.
     */
    Optional<Coupon> findByCodeForUpdate(CouponCode code);
}
