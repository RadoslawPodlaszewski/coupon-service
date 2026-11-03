package com.example.coupons.domain.exception;

import com.example.coupons.domain.model.CouponCode;

public class CountryNotAllowedException extends CouponDomainException {

    public CountryNotAllowedException(CouponCode code) {
        super("Coupon '%s' cannot be used from this country".formatted(code.value()));
    }
}
