package com.example.coupons.domain.rule;

import static com.example.coupons.domain.model.CouponBuilder.aCoupon;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.coupons.domain.exception.CountryNotAllowedException;
import com.example.coupons.domain.model.CountryCode;
import com.example.coupons.domain.model.UserId;

class CountryRuleTest {

    private final CountryRule rule = new CountryRule();
    private final UserId user = new UserId("user-1");

    @Test
    void allowsRedemptionFromTheCouponsCountry() {
        var context = new RedemptionContext(aCoupon().inCountry("PL").build(), user, Optional.of(new CountryCode("PL")));

        assertThatCode(() -> rule.check(context)).doesNotThrowAnyException();
    }

    @Test
    void rejectsAnotherCountry() {
        var context = new RedemptionContext(aCoupon().inCountry("PL").build(), user, Optional.of(new CountryCode("DE")));

        assertThatThrownBy(() -> rule.check(context)).isInstanceOf(CountryNotAllowedException.class);
    }

    @Test
    void rejectsWhenTheCountryIsUnknown() {
        var context = new RedemptionContext(aCoupon().build(), user, Optional.empty());

        assertThatThrownBy(() -> rule.check(context)).isInstanceOf(CountryNotAllowedException.class);
    }
}
