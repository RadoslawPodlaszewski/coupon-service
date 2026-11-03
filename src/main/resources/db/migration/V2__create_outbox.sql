-- Transactional outbox: events are written in the same transaction as the state change,
-- and a relay publishes them afterwards (at-least-once delivery).
CREATE TABLE outbox_events (
    id            UUID          PRIMARY KEY,
    aggregate_id  UUID          NOT NULL,
    event_type    VARCHAR(100)  NOT NULL,
    payload       TEXT          NOT NULL,
    occurred_at   TIMESTAMPTZ   NOT NULL,
    published_at  TIMESTAMPTZ
);

-- Partial index: the relay only ever looks at unpublished rows.
CREATE INDEX ix_outbox_events_unpublished ON outbox_events (occurred_at, id) WHERE published_at IS NULL;
