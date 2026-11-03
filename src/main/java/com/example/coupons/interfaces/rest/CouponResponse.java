package com.example.coupons.interfaces.rest;

import java.time.Instant;

import com.example.coupons.application.query.CouponView;

record CouponResponse(String code, Instant createdAt, int maxUsages, int usageCount,
                      int remainingUsages, String country) {

    static CouponResponse from(CouponView view) {
        return new CouponResponse(view.code(), view.createdAt(), view.maxUsages(), view.usageCount(),
                view.remainingUsages(), view.country());
    }
}
