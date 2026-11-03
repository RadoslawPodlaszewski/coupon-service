package com.example.coupons.domain.repository;

import com.example.coupons.domain.model.CouponId;
import com.example.coupons.domain.model.Redemption;
import com.example.coupons.domain.model.UserId;

public interface RedemptionRepository {

    boolean existsByCouponIdAndUserId(CouponId couponId, UserId userId);

    void save(Redemption redemption);
}
