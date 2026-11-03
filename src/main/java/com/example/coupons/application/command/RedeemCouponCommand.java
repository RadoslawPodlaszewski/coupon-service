package com.example.coupons.application.command;

/** @param idempotencyKey optional client-supplied key; retries with the same key return the original result */
public record RedeemCouponCommand(String code, String userId, String clientIp, String idempotencyKey) {

    public RedeemCouponCommand(String code, String userId, String clientIp) {
        this(code, userId, clientIp, null);
    }
}
