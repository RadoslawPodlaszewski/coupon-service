package com.example.coupons.infrastructure.outbox;

/**
 * Where published events go. Here: a logger. In a real deployment: Kafka, RabbitMQ, SNS, a webhook.
 * Delivery is at-least-once, so consumers must tolerate duplicates.
 */
interface OutboxEventSink {

    void deliver(String eventType, String payload);
}
