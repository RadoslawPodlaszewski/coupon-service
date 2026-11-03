package com.example.coupons.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.coupons.AbstractIntegrationTest;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "coupons.rate-limit.enabled=true",
        "coupons.rate-limit.capacity=2",
        "coupons.rate-limit.refill-tokens=1",
        "coupons.rate-limit.refill-period=1h"})
class RateLimitIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void rejectsRedemptionAttemptsOverTheLimitWithRetryAfter() throws Exception {
        // unknown coupon: answered with 404 before any external call, which is fine for counting attempts
        for (int i = 0; i < 2; i++) {
            attempt().andExpect(status().isNotFound());
        }

        attempt()
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.reason").value("RATE_LIMITED"));
    }

    private org.springframework.test.web.servlet.ResultActions attempt() throws Exception {
        return mvc.perform(post("/api/coupons/{code}/redemptions", "NO-SUCH-COUPON")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"user-1\"}"));
    }
}
