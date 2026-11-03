package com.example.coupons.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.coupons.AbstractIntegrationTest;
import com.example.coupons.application.command.CreateCouponCommand;
import com.example.coupons.application.command.CreateCouponHandler;
import com.example.coupons.application.command.RedeemCouponCommand;
import com.example.coupons.application.command.RedeemCouponHandler;
import com.example.coupons.application.port.ClientCountryResolver;
import com.example.coupons.domain.exception.CountryNotAllowedException;
import com.example.coupons.domain.model.CountryCode;

class OutboxIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    CreateCouponHandler createCoupon;

    @Autowired
    RedeemCouponHandler redeemCoupon;

    @Autowired
    OutboxRelay relay;

    @Autowired
    JdbcClient jdbc;

    @MockitoBean
    ClientCountryResolver clientCountryResolver;

    @MockitoBean
    OutboxEventSink sink;

    @BeforeEach
    void clientIsInPoland() {
        when(clientCountryResolver.resolve(anyString())).thenReturn(Optional.of(new CountryCode("PL")));
    }

    @Test
    void creatingACouponWritesACouponCreatedEvent() {
        String code = uniqueCode();

        createCoupon.handle(new CreateCouponCommand(code, 3, "PL"));

        assertThat(eventsFor("CouponCreated", code)).isEqualTo(1);
    }

    @Test
    void redeemingACouponWritesACouponRedeemedEvent() {
        String code = uniqueCode();
        createCoupon.handle(new CreateCouponCommand(code, 3, "PL"));

        redeemCoupon.handle(new RedeemCouponCommand(code, "user-1", "1.2.3.4"));

        assertThat(eventsFor("CouponRedeemed", code)).isEqualTo(1);
    }

    @Test
    void aFailedRedemptionLeavesNoEventBehind() {
        String code = uniqueCode();
        createCoupon.handle(new CreateCouponCommand(code, 3, "DE"));   // the client is in PL

        assertThatThrownBy(() -> redeemCoupon.handle(new RedeemCouponCommand(code, "user-1", "1.2.3.4")))
                .isInstanceOf(CountryNotAllowedException.class);

        assertThat(eventsFor("CouponRedeemed", code)).isZero();
    }

    @Test
    void theRelayDeliversEachEventOnceAndMarksItPublished() {
        String code = uniqueCode();
        createCoupon.handle(new CreateCouponCommand(code, 3, "PL"));

        drain();
        drain();   // a second run must not deliver it again

        verify(sink, times(1)).deliver(eq("CouponCreated"), contains(code));
        assertThat(unpublishedFor(code)).isZero();
    }

    private void drain() {
        while (relay.publishPending() > 0) {
            // keep going until the outbox is empty (other tests may have left events behind)
        }
    }

    private long eventsFor(String type, String code) {
        return jdbc.sql("SELECT count(*) FROM outbox_events WHERE event_type = :type AND payload LIKE :pattern")
                .param("type", type)
                .param("pattern", "%" + code + "%")
                .query(Long.class)
                .single();
    }

    private long unpublishedFor(String code) {
        return jdbc.sql("SELECT count(*) FROM outbox_events WHERE published_at IS NULL AND payload LIKE :pattern")
                .param("pattern", "%" + code + "%")
                .query(Long.class)
                .single();
    }

    private static String uniqueCode() {
        return ("OUTBOX-" + UUID.randomUUID().toString().substring(0, 8)).toUpperCase();
    }
}
