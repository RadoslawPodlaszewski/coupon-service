package com.example.coupons.domain.model;

import java.util.Objects;

/** Identifier of the user redeeming a coupon (opaque to this service). */
public record UserId(String value) {

    public static final int MAX_LENGTH = 64;

    public UserId {
        Objects.requireNonNull(value, "value");
        value = value.strip();
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("User id must have 1-%d characters".formatted(MAX_LENGTH));
        }
    }
}
