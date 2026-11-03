package com.example.coupons.infrastructure.outbox;

import java.sql.Timestamp;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.coupons.domain.event.DomainEvent;
import com.example.coupons.domain.event.DomainEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Transactional outbox, write half: the event is inserted into the database in the <b>same transaction</b>
 * as the state change. MANDATORY propagation makes it impossible to call this outside one.
 */
@Component
class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final JdbcClient jdbc;
    private final ObjectMapper objectMapper;

    OutboxDomainEventPublisher(JdbcClient jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(DomainEvent event) {
        jdbc.sql("""
                        INSERT INTO outbox_events (id, aggregate_id, event_type, payload, occurred_at)
                        VALUES (:id, :aggregateId, :eventType, :payload, :occurredAt)
                        """)
                .param("id", UUID.randomUUID())
                .param("aggregateId", event.aggregateId())
                .param("eventType", event.getClass().getSimpleName())
                .param("payload", toJson(event))
                .param("occurredAt", Timestamp.from(event.occurredAt()))
                .update();
    }

    private String toJson(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize event " + event, e);
        }
    }
}
