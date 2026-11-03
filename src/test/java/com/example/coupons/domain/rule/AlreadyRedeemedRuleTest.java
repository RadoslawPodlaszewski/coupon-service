package com.example.coupons.domain.rule;

import static com.example.coupons.domain.model.CouponBuilder.aCoupon;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.coupons.domain.exception.CouponAlreadyRedeemedException;
import com.example.coupons.domain.model.CountryCode;
import com.example.coupons.domain.model.UserId;
import com.example.coupons.domain.repository.RedemptionRepository;

class AlreadyRedeemedRuleTest {

    private final RedemptionRepository redemptions = mock(RedemptionRepository.class);
    private final AlreadyRedeemedRule rule = new AlreadyRedeemedRule(redemptions);
    private final RedemptionContext context =
            new RedemptionContext(aCoupon().build(), new UserId("user-1"), Optional.of(new CountryCode("PL")));

    @Test
    void rejectsUserWhoAlreadyRedeemed() {
        when(redemptions.existsByCouponIdAndUserId(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> rule.check(context)).isInstanceOf(CouponAlreadyRedeemedException.class);
    }

    @Test
    void allowsFirstTimeUser() {
        when(redemptions.existsByCouponIdAndUserId(any(), any())).thenReturn(false);

        assertThatCode(() -> rule.check(context)).doesNotThrowAnyException();
    }
}
