package com.example.coupons.domain.event;

import java.time.Instant;
import java.util.UUID;

public record CouponRedeemed(UUID aggregateId, String code, String userId, Instant occurredAt)
        implements DomainEvent {
}
