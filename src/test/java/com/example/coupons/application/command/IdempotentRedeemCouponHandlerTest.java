package com.example.coupons.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.example.coupons.domain.exception.UsageLimitReachedException;
import com.example.coupons.domain.model.CouponCode;

class IdempotentRedeemCouponHandlerTest {

    private final RedeemCouponHandler delegate = mock(RedeemCouponHandler.class);
    private final IdempotencyStore store = mock(IdempotencyStore.class);
    private final IdempotentRedeemCouponHandler handler = new IdempotentRedeemCouponHandler(delegate, store);
    private final RedemptionReceipt receipt =
            new RedemptionReceipt("WIOSNA", "user-1", Instant.parse("2026-01-01T00:00:00Z"));

    private static RedeemCouponCommand command(String key) {
        return new RedeemCouponCommand("wiosna", "user-1", "1.2.3.4", key);
    }

    @Test
    void withoutAKeyItJustDelegates() {
        when(delegate.handle(any())).thenReturn(receipt);

        assertThat(handler.handle(command(null))).isEqualTo(receipt);
        verifyNoInteractions(store);
    }

    @Test
    void aFirstRequestIsExecutedAndItsResultStored() {
        when(store.tryClaim(eq("k1"), anyString())).thenReturn(new ClaimResult.Claimed());
        when(delegate.handle(any())).thenReturn(receipt);

        assertThat(handler.handle(command("k1"))).isEqualTo(receipt);
        verify(store).complete("k1", receipt);
    }

    @Test
    void aRetryReplaysTheStoredResultWithoutExecutingAgain() {
        when(store.tryClaim(eq("k1"), anyString())).thenReturn(new ClaimResult.Replay(receipt));

        assertThat(handler.handle(command("k1"))).isEqualTo(receipt);
        verifyNoInteractions(delegate);
    }

    @Test
    void theSameKeyWithADifferentRequestIsRejected() {
        when(store.tryClaim(eq("k1"), anyString())).thenReturn(new ClaimResult.FingerprintMismatch());

        assertThatThrownBy(() -> handler.handle(command("k1"))).isInstanceOf(IdempotencyKeyReusedException.class);
        verifyNoInteractions(delegate);
    }

    @Test
    void aDuplicateWhileTheFirstIsRunningIsRejected() {
        when(store.tryClaim(eq("k1"), anyString())).thenReturn(new ClaimResult.InProgress());

        assertThatThrownBy(() -> handler.handle(command("k1"))).isInstanceOf(RequestInProgressException.class);
        verifyNoInteractions(delegate);
    }

    @Test
    void aFailedRequestReleasesTheKeySoTheClientCanRetry() {
        when(store.tryClaim(eq("k1"), anyString())).thenReturn(new ClaimResult.Claimed());
        when(delegate.handle(any())).thenThrow(new UsageLimitReachedException(new CouponCode("wiosna")));

        assertThatThrownBy(() -> handler.handle(command("k1"))).isInstanceOf(UsageLimitReachedException.class);

        verify(store).release("k1");
        verify(store, never()).complete(anyString(), any());
    }

    @Test
    void anOverlongKeyIsRejectedBeforeTouchingTheStore() {
        assertThatThrownBy(() -> handler.handle(command("x".repeat(256))))
                .isInstanceOf(InvalidIdempotencyKeyException.class);
        verifyNoInteractions(store, delegate);
    }
}
