package com.example.coupons.domain.exception;

import com.example.coupons.domain.model.CouponCode;

public class CouponNotFoundException extends CouponDomainException {

    public CouponNotFoundException(CouponCode code) {
        super("Coupon '%s' does not exist".formatted(code.value()));
    }
}
