package com.example.coupons.domain.model;

import static com.example.coupons.domain.model.CouponBuilder.aCoupon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.example.coupons.domain.event.CouponCreated;
import com.example.coupons.domain.event.CouponRedeemed;
import com.example.coupons.domain.exception.UsageLimitReachedException;

class CouponTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UserId USER = new UserId("user-1");

    @Test
    void redemptionTakesOneUsageAndReturnsTheFact() {
        Coupon coupon = aCoupon().withLimit(2).build();

        Redemption redemption = coupon.redeem(USER, NOW);

        assertThat(coupon.usageCount()).isEqualTo(1);
        assertThat(redemption.couponId()).isEqualTo(coupon.id());
        assertThat(redemption.userId()).isEqualTo(USER);
    }

    @Test
    void redemptionRecordsAnEvent() {
        Coupon coupon = aCoupon().build();

        coupon.redeem(USER, NOW);

        assertThat(coupon.pullEvents()).singleElement().isInstanceOf(CouponRedeemed.class);
        assertThat(coupon.pullEvents()).isEmpty();   // pulled events are forgotten
    }

    @Test
    void rejectsRedemptionWhenLimitIsReached() {
        Coupon coupon = aCoupon().withLimit(1).alreadyUsed(1).build();

        assertThatThrownBy(() -> coupon.redeem(USER, NOW)).isInstanceOf(UsageLimitReachedException.class);
        assertThat(coupon.usageCount()).isEqualTo(1);
        assertThat(coupon.pullEvents()).isEmpty();   // a failed redemption announces nothing
    }

    @Test
    void creatingACouponRecordsAnEvent() {
        Coupon coupon = Coupon.create(new CouponCode("wiosna"), 5, new CountryCode("PL"), NOW);

        assertThat(coupon.pullEvents()).singleElement().isInstanceOf(CouponCreated.class);
    }

    @Test
    void cannotBeCreatedWithNonPositiveLimit() {
        assertThatThrownBy(() -> aCoupon().withLimit(0).build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannotBeRestoredIntoAStateThatBreaksTheInvariant() {
        assertThatThrownBy(() -> aCoupon().withLimit(3).alreadyUsed(4).build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
