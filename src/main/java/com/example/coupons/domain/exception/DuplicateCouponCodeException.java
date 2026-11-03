package com.example.coupons.domain.exception;

import com.example.coupons.domain.model.CouponCode;

public class DuplicateCouponCodeException extends CouponDomainException {

    public DuplicateCouponCodeException(CouponCode code) {
        super("Coupon '%s' already exists".formatted(code.value()));
    }
}
