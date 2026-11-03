package com.example.coupons.domain.exception;

/** Base type of all business-rule violations raised by the domain. */
public abstract class CouponDomainException extends RuntimeException {

    protected CouponDomainException(String message) {
        super(message);
    }
}
