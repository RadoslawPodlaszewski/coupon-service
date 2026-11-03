package com.example.coupons.application.command;

import java.time.Instant;

/** Minimal confirmation of a successful redemption; not a read model. */
public record RedemptionReceipt(String code, String userId, Instant redeemedAt) {
}
