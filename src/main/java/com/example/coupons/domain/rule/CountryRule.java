package com.example.coupons.domain.rule;

import com.example.coupons.domain.exception.CountryNotAllowedException;

/** A coupon can be used only from its own country; an unknown country counts as not allowed. */
public class CountryRule implements RedemptionRule {

    @Override
    public void check(RedemptionContext context) {
        boolean allowed = context.clientCountry()
                .filter(context.coupon().country()::equals)
                .isPresent();
        if (!allowed) {
            throw new CountryNotAllowedException(context.coupon().code());
        }
    }
}
