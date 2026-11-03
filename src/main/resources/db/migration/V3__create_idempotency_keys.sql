CREATE TABLE idempotency_keys (
    idempotency_key  VARCHAR(255) PRIMARY KEY,
    request_hash     VARCHAR(64)  NOT NULL,   -- fingerprint of the request that claimed the key
    response         TEXT,                     -- NULL while the request is still being processed
    created_at       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_idempotency_keys_created_at ON idempotency_keys (created_at);
