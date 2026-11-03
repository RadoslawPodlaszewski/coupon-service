package com.example.coupons.interfaces.rest.ratelimit;

import java.time.Duration;
import java.util.function.LongSupplier;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * One token bucket per client key (the client IP). State is <b>in memory, per instance</b>: with N instances
 * a client can get up to N times the limit. For a strict cluster-wide limit, keep the buckets in a shared store
 * (Redis, or Bucket4j with a distributed backend); this class is the seam for that.
 */
final class ClientRateLimiter {

    private final RateLimitProperties properties;
    private final LongSupplier nanoClock;
    private final Cache<String, TokenBucket> buckets;

    ClientRateLimiter(RateLimitProperties properties, LongSupplier nanoClock) {
        this.properties = properties;
        this.nanoClock = nanoClock;
        // An idle bucket that has had time to refill completely is equivalent to a brand new one,
        // so evicting it after that time changes nothing and bounds memory.
        Duration fullRefill = Duration.ofNanos(
                properties.refillPeriod().toNanos() * properties.capacity() / properties.refillTokens() + 1);
        this.buckets = Caffeine.newBuilder()
                .maximumSize(properties.maxTrackedClients())
                .expireAfterAccess(fullRefill)
                .build();
    }

    /** @return {@link Duration#ZERO} if the request is allowed, otherwise how long the client should wait */
    Duration tryAcquire(String clientKey) {
        TokenBucket bucket = buckets.get(clientKey, key -> new TokenBucket(
                properties.capacity(), properties.refillTokens(), properties.refillPeriod(), nanoClock));
        return Duration.ofNanos(bucket.nanosUntilAvailable());
    }
}
