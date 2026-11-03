package com.example.coupons.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CouponCodeTest {

    @Test
    void treatsDifferentCasingAsTheSameCode() {
        assertThat(new CouponCode("wiosna")).isEqualTo(new CouponCode("WIOSNA"));
    }

    @Test
    void stripsSurroundingWhitespace() {
        assertThat(new CouponCode("  Wiosna ").value()).isEqualTo("WIOSNA");
    }
}
