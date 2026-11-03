package com.example.coupons.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.coupons.infrastructure.idempotency.JdbcIdempotencyStore;
import com.example.coupons.infrastructure.outbox.OutboxRelay;

/** Background jobs; switch off with {@code coupons.scheduling.enabled=false} (e.g. in tests). */
@Component
@ConditionalOnProperty(name = "coupons.scheduling.enabled", havingValue = "true", matchIfMissing = true)
class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    private final OutboxRelay outboxRelay;
    private final JdbcIdempotencyStore idempotencyStore;

    ScheduledJobs(OutboxRelay outboxRelay, JdbcIdempotencyStore idempotencyStore) {
        this.outboxRelay = outboxRelay;
        this.idempotencyStore = idempotencyStore;
    }

    @Scheduled(fixedDelayString = "${coupons.outbox.poll-interval:PT2S}")
    void relayOutbox() {
        try {
            int published = outboxRelay.publishPending();
            if (published > 0) {
                log.debug("Outbox relay published {} events", published);
            }
        } catch (RuntimeException e) {
            log.warn("Outbox relay failed; events stay in the outbox and will be retried", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    void purgeIdempotencyKeys() {
        int removed = idempotencyStore.purgeExpired();
        if (removed > 0) {
            log.info("Purged {} expired idempotency keys", removed);
        }
    }
}
