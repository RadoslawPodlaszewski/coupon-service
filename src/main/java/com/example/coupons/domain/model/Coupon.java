package com.example.coupons.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.example.coupons.domain.event.CouponCreated;
import com.example.coupons.domain.event.CouponRedeemed;
import com.example.coupons.domain.event.DomainEvent;
import com.example.coupons.domain.exception.UsageLimitReachedException;

/**
 * Aggregate root. It owns the invariant that always holds, whoever calls it: the usage count
 * never exceeds the limit. Redemption <em>policies</em> (country, one redemption per user) are
 * separate {@link com.example.coupons.domain.rule.RedemptionRule}s, checked by the
 * {@link com.example.coupons.domain.service.CouponRedeemer} domain service.
 * <p>
 * Records domain events as things happen; the application layer pulls and publishes them.
 * Pure Java on purpose: no framework annotations. Concurrent redemptions are serialized by the
 * application layer, which loads the aggregate under a pessimistic lock.
 */
public final class Coupon {

    private final CouponId id;
    private final CouponCode code;
    private final Instant createdAt;
    private final int maxUsages;
    private final CountryCode country;
    private int usageCount;
    private final List<DomainEvent> events = new ArrayList<>();

    private Coupon(CouponId id, CouponCode code, Instant createdAt, int maxUsages, int usageCount, CountryCode country) {
        if (maxUsages < 1) {
            throw new IllegalArgumentException("maxUsages must be positive");
        }
        if (usageCount < 0 || usageCount > maxUsages) {
            throw new IllegalArgumentException("usageCount must be between 0 and maxUsages");
        }
        this.id = Objects.requireNonNull(id);
        this.code = Objects.requireNonNull(code);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.maxUsages = maxUsages;
        this.usageCount = usageCount;
        this.country = Objects.requireNonNull(country);
    }

    /** Factory for a brand new coupon; records {@link CouponCreated}. */
    public static Coupon create(CouponCode code, int maxUsages, CountryCode country, Instant now) {
        Coupon coupon = new Coupon(CouponId.generate(), code, now, maxUsages, 0, country);
        coupon.events.add(new CouponCreated(coupon.id.value(), code.value(), maxUsages, country.value(), now));
        return coupon;
    }

    /** Rebuilds an existing coupon from persisted state; records no events. */
    public static Coupon restore(CouponId id, CouponCode code, Instant createdAt,
                                 int maxUsages, int usageCount, CountryCode country) {
        return new Coupon(id, code, createdAt, maxUsages, usageCount, country);
    }

    /**
     * Takes one usage of this coupon and records {@link CouponRedeemed}.
     *
     * @throws UsageLimitReachedException if no usages are left
     */
    public Redemption redeem(UserId userId, Instant now) {
        if (usageCount >= maxUsages) {
            throw new UsageLimitReachedException(code);
        }
        usageCount++;
        events.add(new CouponRedeemed(id.value(), code.value(), userId.value(), now));
        return new Redemption(id, userId, now);
    }

    /** Returns the events recorded since the last call and forgets them. */
    public List<DomainEvent> pullEvents() {
        List<DomainEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    public CouponId id() {
        return id;
    }

    public CouponCode code() {
        return code;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public int maxUsages() {
        return maxUsages;
    }

    public int usageCount() {
        return usageCount;
    }

    public CountryCode country() {
        return country;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Coupon other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
