package com.example.coupons.domain.rule;

import com.example.coupons.domain.exception.CouponAlreadyRedeemedException;
import com.example.coupons.domain.repository.RedemptionRepository;

/**
 * One redemption per user. Safe from check-then-act races only because the caller holds the
 * coupon's pessimistic lock, so no other redemption of this coupon runs in parallel.
 */
public class AlreadyRedeemedRule implements RedemptionRule {

    private final RedemptionRepository redemptions;

    public AlreadyRedeemedRule(RedemptionRepository redemptions) {
        this.redemptions = redemptions;
    }

    @Override
    public void check(RedemptionContext context) {
        if (redemptions.existsByCouponIdAndUserId(context.coupon().id(), context.userId())) {
            throw new CouponAlreadyRedeemedException(context.coupon().code(), context.userId());
        }
    }
}
