package com.example.coupons.domain.exception;

import com.example.coupons.domain.model.CouponCode;

public class UsageLimitReachedException extends CouponDomainException {

    public UsageLimitReachedException(CouponCode code) {
        super("Coupon '%s' has reached its maximum number of usages".formatted(code.value()));
    }
}
