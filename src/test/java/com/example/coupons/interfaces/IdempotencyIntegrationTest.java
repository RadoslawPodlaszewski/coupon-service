package com.example.coupons.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.coupons.AbstractIntegrationTest;
import com.example.coupons.application.command.CreateCouponCommand;
import com.example.coupons.application.command.CreateCouponHandler;
import com.example.coupons.application.port.ClientCountryResolver;
import com.example.coupons.domain.model.CountryCode;

@AutoConfigureMockMvc
class IdempotencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CreateCouponHandler createCoupon;

    @MockitoBean
    ClientCountryResolver clientCountryResolver;

    @BeforeEach
    void clientIsInPoland() {
        when(clientCountryResolver.resolve(anyString())).thenReturn(Optional.of(new CountryCode("PL")));
    }

    @Test
    void aRetryWithTheSameKeyReplaysTheOriginalResponseAndRedeemsOnlyOnce() throws Exception {
        String code = newCoupon("PL");
        String key = UUID.randomUUID().toString();

        String first = redeem(code, "user-1", key)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String retry = redeem(code, "user-1", key)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        assertThat(retry).isEqualTo(first);
        mvc.perform(get("/api/coupons/{code}", code)).andExpect(jsonPath("$.usageCount").value(1));
    }

    @Test
    void withoutAKeyARetryIsStillRejectedAsAlreadyRedeemed() throws Exception {
        String code = newCoupon("PL");

        mvc.perform(post("/api/coupons/{code}/redemptions", code)
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"user-1\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/coupons/{code}/redemptions", code)
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"user-1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("COUPON_ALREADY_REDEEMED"));
    }

    @Test
    void theSameKeyForADifferentRequestIsRejected() throws Exception {
        String code = newCoupon("PL");
        String key = UUID.randomUUID().toString();
        redeem(code, "user-1", key).andExpect(status().isCreated());

        redeem(code, "user-2", key)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.reason").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void aFailedRequestDoesNotBurnTheKey() throws Exception {
        String code = newCoupon("DE");   // the client is in PL, so the first attempt fails
        String key = UUID.randomUUID().toString();
        redeem(code, "user-1", key).andExpect(status().isForbidden());

        when(clientCountryResolver.resolve(anyString())).thenReturn(Optional.of(new CountryCode("DE")));

        redeem(code, "user-1", key).andExpect(status().isCreated());
    }

    private String newCoupon(String country) {
        String code = ("IDEM-" + UUID.randomUUID().toString().substring(0, 8)).toUpperCase();
        createCoupon.handle(new CreateCouponCommand(code, 5, country));
        return code;
    }

    private ResultActions redeem(String code, String userId, String key) throws Exception {
        return mvc.perform(post("/api/coupons/{code}/redemptions", code)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", key)
                .content("{\"userId\":\"%s\"}".formatted(userId)));
    }
}
