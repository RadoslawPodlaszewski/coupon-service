package com.example.coupons.infrastructure.idempotency;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.coupons.application.command.ClaimResult;
import com.example.coupons.application.command.IdempotencyStore;
import com.example.coupons.application.command.RedemptionReceipt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Postgres-backed idempotency store. Each method is its own short transaction, so a claim is
 * committed (and visible to concurrent duplicates) before the real work starts.
 * <p>
 * Claiming relies on the primary key: {@code INSERT ... ON CONFLICT DO NOTHING} lets exactly one
 * concurrent request win. A claim that never completed (process crash) is taken over once it is
 * older than {@code in-progress-timeout}.
 */
@Repository
public class JdbcIdempotencyStore implements IdempotencyStore {

    private static final int MAX_CLAIM_ATTEMPTS = 3;

    private final JdbcClient jdbc;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final IdempotencyProperties properties;

    JdbcIdempotencyStore(JdbcClient jdbc, ObjectMapper objectMapper, Clock clock, IdempotencyProperties properties) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.properties = properties;
    }

    @Override
    @Transactional
    public ClaimResult tryClaim(String key, String requestFingerprint) {
        Instant now = clock.instant();
        for (int attempt = 0; attempt < MAX_CLAIM_ATTEMPTS; attempt++) {
            int inserted = jdbc.sql("""
                            INSERT INTO idempotency_keys (idempotency_key, request_hash, created_at)
                            VALUES (:key, :hash, :now)
                            ON CONFLICT (idempotency_key) DO NOTHING
                            """)
                    .param("key", key)
                    .param("hash", requestFingerprint)
                    .param("now", Timestamp.from(now))
                    .update();
            if (inserted == 1) {
                return new ClaimResult.Claimed();
            }

            Optional<StoredKey> existing = jdbc.sql("""
                            SELECT request_hash, response, created_at
                              FROM idempotency_keys
                             WHERE idempotency_key = :key
                               FOR UPDATE
                            """)
                    .param("key", key)
                    .query((rs, rowNum) -> new StoredKey(
                            rs.getString("request_hash"),
                            rs.getString("response"),
                            rs.getTimestamp("created_at").toInstant()))
                    .optional();
            if (existing.isEmpty()) {
                continue;   // released in the meantime: try to claim it again
            }

            StoredKey stored = existing.get();
            if (!stored.requestHash().equals(requestFingerprint)) {
                return new ClaimResult.FingerprintMismatch();
            }
            if (stored.response() != null) {
                return new ClaimResult.Replay(fromJson(stored.response()));
            }
            if (stored.createdAt().isBefore(now.minus(properties.inProgressTimeout()))) {
                jdbc.sql("UPDATE idempotency_keys SET created_at = :now WHERE idempotency_key = :key")
                        .param("now", Timestamp.from(now))
                        .param("key", key)
                        .update();
                return new ClaimResult.Claimed();   // abandoned claim taken over
            }
            return new ClaimResult.InProgress();
        }
        return new ClaimResult.InProgress();
    }

    @Override
    @Transactional
    public void complete(String key, RedemptionReceipt receipt) {
        jdbc.sql("UPDATE idempotency_keys SET response = :response WHERE idempotency_key = :key")
                .param("response", toJson(receipt))
                .param("key", key)
                .update();
    }

    @Override
    @Transactional
    public void release(String key) {
        jdbc.sql("DELETE FROM idempotency_keys WHERE idempotency_key = :key AND response IS NULL")
                .param("key", key)
                .update();
    }

    /** Housekeeping: drops keys older than the retention period. Returns how many were removed. */
    @Transactional
    public int purgeExpired() {
        Instant cutoff = clock.instant().minus(properties.retention());
        return jdbc.sql("DELETE FROM idempotency_keys WHERE created_at < :cutoff")
                .param("cutoff", Timestamp.from(cutoff))
                .update();
    }

    private String toJson(RedemptionReceipt receipt) {
        try {
            return objectMapper.writeValueAsString(receipt);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize receipt", e);
        }
    }

    private RedemptionReceipt fromJson(String json) {
        try {
            return objectMapper.readValue(json, RedemptionReceipt.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read stored receipt", e);
        }
    }

    private record StoredKey(String requestHash, String response, Instant createdAt) {
    }
}
