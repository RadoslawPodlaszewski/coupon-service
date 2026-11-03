package com.example.coupons.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Identity of the {@link Coupon} aggregate. Generated in the domain, so an aggregate always has an id. */
public record CouponId(UUID value) {

    public CouponId {
        Objects.requireNonNull(value, "value");
    }

    public static CouponId generate() {
        return new CouponId(UUID.randomUUID());
    }
}
