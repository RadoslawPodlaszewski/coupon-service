package com.example.coupons.application.command;

/** Outcome of trying to claim an idempotency key. */
public sealed interface ClaimResult {

    /** The key is ours: process the request. */
    record Claimed() implements ClaimResult {
    }

    /** The request already succeeded earlier: return the stored result. */
    record Replay(RedemptionReceipt receipt) implements ClaimResult {
    }

    /** The key was used before with a different request. */
    record FingerprintMismatch() implements ClaimResult {
    }

    /** Another request with this key is being processed right now. */
    record InProgress() implements ClaimResult {
    }
}
