package com.example.coupons.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.coupons.AbstractIntegrationTest;
import com.example.coupons.application.command.CreateCouponHandler;
import com.example.coupons.application.query.GetCouponHandler;
import com.example.coupons.application.query.GetCouponQuery;
import com.example.coupons.application.command.RedeemCouponHandler;
import com.example.coupons.application.command.CreateCouponCommand;
import com.example.coupons.application.command.RedeemCouponCommand;
import com.example.coupons.application.port.ClientCountryResolver;
import com.example.coupons.domain.exception.CouponAlreadyRedeemedException;
import com.example.coupons.domain.exception.DuplicateCouponCodeException;
import com.example.coupons.domain.exception.UsageLimitReachedException;
import com.example.coupons.domain.model.CountryCode;

/** Hammers one coupon from many threads at once and checks that the business invariants survive. */
class CouponRedemptionConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    CreateCouponHandler createCoupon;

    @Autowired
    RedeemCouponHandler redeemCoupon;

    @Autowired
    GetCouponHandler getCoupon;

    @MockitoBean
    ClientCountryResolver clientCountryResolver;

    @BeforeEach
    void clientIsInPoland() {
        when(clientCountryResolver.resolve(anyString())).thenReturn(Optional.of(new CountryCode("PL")));
    }

    @Test
    void neverExceedsMaxUsagesWhenManyUsersRedeemAtOnce() throws Exception {
        String code = "race-" + UUID.randomUUID().toString().substring(0, 8);
        createCoupon.handle(new CreateCouponCommand(code, 5, "PL"));

        List<String> outcomes = runConcurrently(40, i -> () -> redeem(code, "user-" + i));

        assertThat(outcomes.stream().filter("REDEEMED"::equals)).hasSize(5);
        assertThat(outcomes.stream().filter("LIMIT_REACHED"::equals)).hasSize(35);
        assertThat(getCoupon.handle(new GetCouponQuery(code)).usageCount()).isEqualTo(5);
    }

    @Test
    void oneUserCanRedeemOnlyOnceEvenWithParallelRequests() throws Exception {
        String code = "same-" + UUID.randomUUID().toString().substring(0, 8);
        createCoupon.handle(new CreateCouponCommand(code, 100, "PL"));

        List<String> outcomes = runConcurrently(20, i -> () -> redeem(code, "the-same-user"));

        assertThat(outcomes.stream().filter("REDEEMED"::equals)).hasSize(1);
        assertThat(outcomes.stream().filter("ALREADY_REDEEMED"::equals)).hasSize(19);
        assertThat(getCoupon.handle(new GetCouponQuery(code)).usageCount()).isEqualTo(1);
    }

    @Test
    void creatingTheSameCodeConcurrentlySucceedsExactlyOnce() throws Exception {
        String raw = "dup-" + UUID.randomUUID().toString().substring(0, 8);

        List<Boolean> created = runConcurrently(10, i -> () -> {
            try {
                // alternate casing: still the same coupon
                createCoupon.handle(new CreateCouponCommand(i % 2 == 0 ? raw : raw.toUpperCase(), 1, "PL"));
                return true;
            } catch (DuplicateCouponCodeException e) {
                return false;
            }
        });

        assertThat(created.stream().filter(Boolean::booleanValue)).hasSize(1);
    }

    private String redeem(String code, String userId) {
        try {
            redeemCoupon.handle(new RedeemCouponCommand(code, userId, "1.2.3.4"));
            return "REDEEMED";
        } catch (UsageLimitReachedException e) {
            return "LIMIT_REACHED";
        } catch (CouponAlreadyRedeemedException e) {
            return "ALREADY_REDEEMED";
        }
    }

    /** Starts all tasks behind a latch so they hit the database at (nearly) the same moment. */
    private static <T> List<T> runConcurrently(int threads, IntFunction<Callable<T>> taskFactory) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Callable<T> task = taskFactory.apply(i);
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
