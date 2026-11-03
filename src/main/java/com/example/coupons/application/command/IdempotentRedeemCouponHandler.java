package com.example.coupons.application.command;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Service;

import com.example.coupons.application.CommandHandler;
import com.example.coupons.domain.model.CouponCode;
import com.example.coupons.domain.model.UserId;

/**
 * Decorator adding idempotency to redemption. With an {@code Idempotency-Key}, a client that retries
 * after a timeout gets the original receipt instead of "already redeemed".
 * <ul>
 *     <li>only <b>successful</b> results are stored; a failed request releases its key, so retrying is allowed;</li>
 *     <li>the same key with a different request is rejected (422), never silently replayed;</li>
 *     <li>a duplicate that arrives while the first request is still running gets 409.</li>
 * </ul>
 */
@Service
public class IdempotentRedeemCouponHandler implements CommandHandler<RedeemCouponCommand, RedemptionReceipt> {

    static final int MAX_KEY_LENGTH = 255;

    private final RedeemCouponHandler delegate;
    private final IdempotencyStore store;

    public IdempotentRedeemCouponHandler(RedeemCouponHandler delegate, IdempotencyStore store) {
        this.delegate = delegate;
        this.store = store;
    }

    @Override
    public RedemptionReceipt handle(RedeemCouponCommand command) {
        String rawKey = command.idempotencyKey();
        if (rawKey == null || rawKey.isBlank()) {
            return delegate.handle(command);
        }
        String key = rawKey.strip();
        if (key.length() > MAX_KEY_LENGTH) {
            throw new InvalidIdempotencyKeyException(
                    "Idempotency-Key must not be longer than %d characters".formatted(MAX_KEY_LENGTH));
        }

        return switch (store.tryClaim(key, fingerprint(command))) {
            case ClaimResult.Replay replay -> replay.receipt();
            case ClaimResult.FingerprintMismatch mismatch -> throw new IdempotencyKeyReusedException();
            case ClaimResult.InProgress inProgress -> throw new RequestInProgressException();
            case ClaimResult.Claimed claimed -> execute(key, command);
        };
    }

    private RedemptionReceipt execute(String key, RedeemCouponCommand command) {
        RedemptionReceipt receipt;
        try {
            receipt = delegate.handle(command);
        } catch (RuntimeException e) {
            store.release(key);
            throw e;
        }
        // If this fails, the claim stays "in progress" until it times out; a retry then hits
        // "already redeemed" rather than redeeming twice. The business invariant is never at risk.
        store.complete(key, receipt);
        return receipt;
    }

    /** Identifies "the same request": same coupon, same user. The client IP is deliberately excluded. */
    private static String fingerprint(RedeemCouponCommand command) {
        String canonical = new CouponCode(command.code()).value() + "|" + new UserId(command.userId()).value();
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
