package com.example.coupons.domain.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Natural key of a coupon. Normalization lives here, in one place, so "WIOSNA", "wiosna"
 * and " Wiosna " are always the same coupon ({@link Locale#ROOT} avoids locale surprises).
 */
public record CouponCode(String value) {

    public CouponCode {
        Objects.requireNonNull(value, "value");
        value = value.strip().toUpperCase(Locale.ROOT);
    }
}
