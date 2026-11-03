package com.example.coupons.interfaces.rest;

import java.time.Instant;

import com.example.coupons.application.command.RedemptionReceipt;

record RedemptionResponse(String code, String userId, Instant redeemedAt) {

    static RedemptionResponse from(RedemptionReceipt receipt) {
        return new RedemptionResponse(receipt.code(), receipt.userId(), receipt.redeemedAt());
    }
}
