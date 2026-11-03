package com.example.coupons.domain.rule;

/**
 * One business policy that must hold for a redemption to be allowed. Adding a policy
 * (expiry date, minimum order value, ...) means adding a class and registering it; existing
 * rules and the aggregate stay untouched (open/closed principle).
 */
public interface RedemptionRule {

    /** @throws com.example.coupons.domain.exception.CouponDomainException if the redemption is not allowed */
    void check(RedemptionContext context);
}
