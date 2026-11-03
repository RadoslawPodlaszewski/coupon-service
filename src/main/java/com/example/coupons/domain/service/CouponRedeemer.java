package com.example.coupons.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.example.coupons.domain.model.CountryCode;
import com.example.coupons.domain.model.Coupon;
import com.example.coupons.domain.model.Redemption;
import com.example.coupons.domain.model.UserId;
import com.example.coupons.domain.rule.RedemptionContext;
import com.example.coupons.domain.rule.RedemptionRule;

/**
 * Domain service: runs the redemption policies in order, then lets the aggregate take the usage
 * (which enforces the usage-limit invariant itself).
 */
public class CouponRedeemer {

    private final List<RedemptionRule> rules;

    public CouponRedeemer(List<RedemptionRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public Redemption redeem(Coupon coupon, UserId userId, Optional<CountryCode> clientCountry, Instant now) {
        RedemptionContext context = new RedemptionContext(coupon, userId, clientCountry);
        rules.forEach(rule -> rule.check(context));
        return coupon.redeem(userId, now);
    }
}
