package com.example.coupons.domain.exception;

import com.example.coupons.domain.model.CouponCode;
import com.example.coupons.domain.model.UserId;

public class CouponAlreadyRedeemedException extends CouponDomainException {

    public CouponAlreadyRedeemedException(CouponCode code, UserId userId) {
        super("User '%s' has already redeemed coupon '%s'".formatted(userId.value(), code.value()));
    }
}
