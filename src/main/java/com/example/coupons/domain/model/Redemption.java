package com.example.coupons.domain.model;

import java.time.Instant;

/** Immutable fact: this user redeemed this coupon at this moment. */
public record Redemption(CouponId couponId, UserId userId, Instant redeemedAt) {
}
