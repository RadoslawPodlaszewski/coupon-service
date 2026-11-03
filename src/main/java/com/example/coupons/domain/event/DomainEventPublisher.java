package com.example.coupons.domain.event;

/**
 * Port for publishing domain events. The implementation must make publication atomic with the
 * state change (see the transactional outbox adapter), otherwise "saved but never announced"
 * or "announced but rolled back" becomes possible.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
