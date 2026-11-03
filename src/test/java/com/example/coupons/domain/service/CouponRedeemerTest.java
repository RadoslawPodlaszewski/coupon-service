package com.example.coupons.domain.service;

import static com.example.coupons.domain.model.CouponBuilder.aCoupon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.example.coupons.domain.exception.CountryNotAllowedException;
import com.example.coupons.domain.model.CountryCode;
import com.example.coupons.domain.model.Coupon;
import com.example.coupons.domain.model.UserId;
import com.example.coupons.domain.rule.RedemptionRule;

class CouponRedeemerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final RedemptionRule first = mock(RedemptionRule.class);
    private final RedemptionRule second = mock(RedemptionRule.class);
    private final CouponRedeemer redeemer = new CouponRedeemer(List.of(first, second));
    private final Coupon coupon = aCoupon().build();
    private final UserId user = new UserId("user-1");
    private final Optional<CountryCode> country = Optional.of(new CountryCode("PL"));

    @Test
    void runsAllRulesInOrderThenRedeems() {
        redeemer.redeem(coupon, user, country, NOW);

        InOrder order = inOrder(first, second);
        order.verify(first).check(any());
        order.verify(second).check(any());
        assertThat(coupon.usageCount()).isEqualTo(1);
    }

    @Test
    void stopsAtTheFirstViolationAndLeavesTheCouponUntouched() {
        doThrow(new CountryNotAllowedException(coupon.code())).when(first).check(any());

        assertThatThrownBy(() -> redeemer.redeem(coupon, user, country, NOW))
                .isInstanceOf(CountryNotAllowedException.class);

        verifyNoInteractions(second);
        assertThat(coupon.usageCount()).isZero();
    }
}
