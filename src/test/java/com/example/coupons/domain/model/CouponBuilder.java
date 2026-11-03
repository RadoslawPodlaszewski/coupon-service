package com.example.coupons.domain.model;

import java.time.Instant;

/**
 * Test Data Builder: sensible defaults, and each test spells out only what matters to it
 * ({@code aCoupon().withLimit(1).inCountry("DE").build()}). Builds through {@link Coupon#restore},
 * i.e. like a coupon loaded from the database: no pending domain events.
 */
public final class CouponBuilder {

    private String code = "WIOSNA";
    private int maxUsages = 10;
    private int usageCount = 0;
    private String country = "PL";
    private Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

    private CouponBuilder() {
    }

    public static CouponBuilder aCoupon() {
        return new CouponBuilder();
    }

    public CouponBuilder withCode(String code) {
        this.code = code;
        return this;
    }

    public CouponBuilder withLimit(int maxUsages) {
        this.maxUsages = maxUsages;
        return this;
    }

    public CouponBuilder alreadyUsed(int usageCount) {
        this.usageCount = usageCount;
        return this;
    }

    public CouponBuilder inCountry(String country) {
        this.country = country;
        return this;
    }

    public Coupon build() {
        return Coupon.restore(CouponId.generate(), new CouponCode(code), createdAt, maxUsages, usageCount,
                new CountryCode(country));
    }
}
