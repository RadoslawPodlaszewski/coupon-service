package com.example.coupons.infrastructure.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class LoggingOutboxEventSink implements OutboxEventSink {

    private static final Logger log = LoggerFactory.getLogger(LoggingOutboxEventSink.class);

    @Override
    public void deliver(String eventType, String payload) {
        log.info("Domain event published: {} {}", eventType, payload);
    }
}
