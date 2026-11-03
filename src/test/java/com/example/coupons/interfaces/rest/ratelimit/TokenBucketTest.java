package com.example.coupons.interfaces.rest.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class TokenBucketTest {

    private final AtomicLong nanos = new AtomicLong(1_000);
    /** capacity 2, refills one token every 10 seconds */
    private final TokenBucket bucket = new TokenBucket(2, 1, Duration.ofSeconds(10), nanos::get);

    private void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }

    @Test
    void allowsABurstUpToCapacityThenRejects() {
        assertThat(bucket.nanosUntilAvailable()).isZero();
        assertThat(bucket.nanosUntilAvailable()).isZero();

        assertThat(bucket.nanosUntilAvailable()).isEqualTo(Duration.ofSeconds(10).toNanos());
    }

    @Test
    void refillsOverTime() {
        bucket.nanosUntilAvailable();
        bucket.nanosUntilAvailable();

        advance(Duration.ofSeconds(10));

        assertThat(bucket.nanosUntilAvailable()).isZero();
        assertThat(bucket.nanosUntilAvailable()).isPositive();
    }

    @Test
    void reportsTheRemainingWaitAfterAPartialRefill() {
        bucket.nanosUntilAvailable();
        bucket.nanosUntilAvailable();

        advance(Duration.ofSeconds(4));

        assertThat(bucket.nanosUntilAvailable()).isEqualTo(Duration.ofSeconds(6).toNanos());
    }

    @Test
    void neverAccumulatesMoreThanCapacity() {
        advance(Duration.ofDays(1));

        assertThat(bucket.nanosUntilAvailable()).isZero();
        assertThat(bucket.nanosUntilAvailable()).isZero();
        assertThat(bucket.nanosUntilAvailable()).isPositive();
    }
}
