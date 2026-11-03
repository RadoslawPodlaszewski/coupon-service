package com.example.coupons.domain.event;

import java.time.Instant;
import java.util.UUID;

public record CouponCreated(UUID aggregateId, String code, int maxUsages, String country, Instant occurredAt)
        implements DomainEvent {
}
