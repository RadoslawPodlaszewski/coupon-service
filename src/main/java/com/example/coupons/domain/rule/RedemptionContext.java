package com.example.coupons.domain.rule;

import java.util.Optional;

import com.example.coupons.domain.model.CountryCode;
import com.example.coupons.domain.model.Coupon;
import com.example.coupons.domain.model.UserId;

/** Everything a rule may need to decide whether a redemption is allowed. */
public record RedemptionContext(Coupon coupon, UserId userId, Optional<CountryCode> clientCountry) {
}
