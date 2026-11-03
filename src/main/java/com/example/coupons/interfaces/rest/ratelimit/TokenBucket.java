package com.example.coupons.interfaces.rest.ratelimit;

import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * Token bucket: holds up to {@code capacity} tokens (the allowed burst) and refills
 * {@code refillTokens} tokens per {@code refillPeriod}. Each request costs one token.
 * <p>
 * Integer arithmetic only (no floating point drift): one token is {@code period} "units" and the
 * bucket gains {@code refillTokens} units per nanosecond. The clock is injectable for tests.
 */
final class TokenBucket {

    private final long capacityUnits;
    private final long costUnits;
    private final long refillTokens;
    private final long fullRefillNanos;
    private final LongSupplier nanoClock;

    private long units;
    private long lastRefillNanos;

    TokenBucket(long capacity, long refillTokens, Duration refillPeriod, LongSupplier nanoClock) {
        long periodNanos = refillPeriod.toNanos();
        this.capacityUnits = capacity * periodNanos;
        this.costUnits = periodNanos;
        this.refillTokens = refillTokens;
        this.fullRefillNanos = capacityUnits / refillTokens + 1;
        this.nanoClock = nanoClock;
        this.units = capacityUnits;
        this.lastRefillNanos = nanoClock.getAsLong();
    }

    /** @return 0 if a token was taken, otherwise the nanoseconds to wait until one is available */
    synchronized long nanosUntilAvailable() {
        long now = nanoClock.getAsLong();
        long elapsed = Math.min(Math.max(now - lastRefillNanos, 0), fullRefillNanos);
        lastRefillNanos = now;
        units = Math.min(capacityUnits, units + elapsed * refillTokens);

        if (units >= costUnits) {
            units -= costUnits;
            return 0;
        }
        long missing = costUnits - units;
        return (missing + refillTokens - 1) / refillTokens;
    }
}
