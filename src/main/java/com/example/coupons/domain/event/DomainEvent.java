package com.example.coupons.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Something that happened in the domain, named in the past tense. Immutable. */
public interface DomainEvent {

    UUID aggregateId();

    Instant occurredAt();
}
