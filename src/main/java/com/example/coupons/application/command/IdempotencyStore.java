package com.example.coupons.application.command;

/** Outbound port: remembers which idempotency keys were seen and what they produced. */
public interface IdempotencyStore {

    ClaimResult tryClaim(String key, String requestFingerprint);

    void complete(String key, RedemptionReceipt receipt);

    /** Forgets a claim whose request failed, so the client can retry with the same key. */
    void release(String key);
}
