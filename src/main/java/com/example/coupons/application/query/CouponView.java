package com.example.coupons.application.query;

import java.time.Instant;

/**
 * Read model: shaped for the consumer, not for the aggregate. It contains derived data
 * ({@code remainingUsages}) that the write model neither stores nor needs.
 */
public record CouponView(String code, Instant createdAt, int maxUsages, int usageCount,
                         int remainingUsages, String country) {
}
