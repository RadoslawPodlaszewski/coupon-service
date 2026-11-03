CREATE TABLE coupons (
    id            UUID         PRIMARY KEY,   -- assigned by the domain, not by the database
    code          VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    max_usages    INTEGER      NOT NULL,
    usage_count   INTEGER      NOT NULL DEFAULT 0,
    country_code  VARCHAR(2)   NOT NULL,

    -- Codes are stored normalized (upper case), so UNIQUE is case-insensitive uniqueness.
    CONSTRAINT uq_coupons_code             UNIQUE (code),
    CONSTRAINT ck_coupons_code_normalized  CHECK (code = UPPER(code)),
    CONSTRAINT ck_coupons_max_usages       CHECK (max_usages > 0),
    -- Backstop for the aggregate invariant "usage count never exceeds the limit".
    CONSTRAINT ck_coupons_usage_count      CHECK (usage_count >= 0 AND usage_count <= max_usages)
);

CREATE TABLE coupon_usages (
    id         BIGSERIAL    PRIMARY KEY,
    coupon_id  UUID         NOT NULL REFERENCES coupons (id),
    user_id    VARCHAR(64)  NOT NULL,
    used_at    TIMESTAMPTZ  NOT NULL,

    -- Backstop for "one user can redeem a coupon only once".
    CONSTRAINT uq_coupon_usages_coupon_user UNIQUE (coupon_id, user_id)
);
