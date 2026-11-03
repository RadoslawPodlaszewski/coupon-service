package com.example.coupons.infrastructure.outbox;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Transactional outbox, read half: hands unpublished events to the sink and marks them published.
 * <ul>
 *     <li><b>At-least-once:</b> if the sink fails, the transaction rolls back and the events are retried.
 *         If the process dies after delivery but before commit, they are delivered again.</li>
 *     <li><b>Safe with several instances:</b> {@code FOR UPDATE SKIP LOCKED} lets each relay take different rows.
 *         The price is that ordering across relays is not guaranteed, so consumers must not depend on it.</li>
 * </ul>
 */
@Component
public class OutboxRelay {

    private final JdbcClient jdbc;
    private final TransactionTemplate transactionTemplate;
    private final OutboxEventSink sink;
    private final OutboxProperties properties;

    OutboxRelay(JdbcClient jdbc, TransactionTemplate transactionTemplate, OutboxEventSink sink,
                OutboxProperties properties) {
        this.jdbc = jdbc;
        this.transactionTemplate = transactionTemplate;
        this.sink = sink;
        this.properties = properties;
    }

    /** Publishes one batch and returns how many events it handled. */
    public int publishPending() {
        Integer published = transactionTemplate.execute(status -> {
            List<PendingEvent> pending = jdbc.sql("""
                            SELECT id, event_type, payload
                              FROM outbox_events
                             WHERE published_at IS NULL
                             ORDER BY occurred_at, id
                             LIMIT :limit
                               FOR UPDATE SKIP LOCKED
                            """)
                    .param("limit", properties.batchSize())
                    .query((rs, rowNum) -> new PendingEvent(
                            rs.getObject("id", UUID.class),
                            rs.getString("event_type"),
                            rs.getString("payload")))
                    .list();

            for (PendingEvent event : pending) {
                sink.deliver(event.eventType(), event.payload());
                jdbc.sql("UPDATE outbox_events SET published_at = now() WHERE id = :id")
                        .param("id", event.id())
                        .update();
            }
            return pending.size();
        });
        return published == null ? 0 : published;
    }

    private record PendingEvent(UUID id, String eventType, String payload) {
    }
}
