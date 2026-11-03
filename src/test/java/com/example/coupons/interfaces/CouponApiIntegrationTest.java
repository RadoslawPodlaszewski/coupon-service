package com.example.coupons.interfaces;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import com.example.coupons.application.port.ClientCountryResolver;
import com.example.coupons.application.port.ClientCountryUnavailableException;
import com.example.coupons.domain.model.CountryCode;

@AutoConfigureMockMvc
class CouponApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ClientCountryResolver clientCountryResolver;

    @BeforeEach
    void clientIsInPoland() {
        when(clientCountryResolver.resolve(anyString())).thenReturn(Optional.of(new CountryCode("PL")));
    }

    // ---- creating coupons ----

    @Test
    void createsCouponWithNormalizedCodeAndCountry() throws Exception {
        String code = uniqueCode("wiosna");

        createCoupon(code, 2, "pl")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/coupons/" + code.toUpperCase())))
                .andExpect(jsonPath("$.code").value(code.toUpperCase()))
                .andExpect(jsonPath("$.country").value("PL"))
                .andExpect(jsonPath("$.maxUsages").value(2))
                .andExpect(jsonPath("$.usageCount").value(0))
                .andExpect(jsonPath("$.remainingUsages").value(2))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void rejectsDuplicateCodeRegardlessOfCase() throws Exception {
        String code = uniqueCode("lato");
        createCoupon(code.toLowerCase(), 1, "PL").andExpect(status().isCreated());

        createCoupon(code.toUpperCase(), 5, "DE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("COUPON_CODE_ALREADY_EXISTS"));
    }

    @Test
    void rejectsInvalidPayload() throws Exception {
        createCoupon("ab", 0, "XX")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.code").exists())
                .andExpect(jsonPath("$.errors.maxUsages").exists())
                .andExpect(jsonPath("$.errors.countryValid").exists());
    }

    // ---- redeeming coupons ----

    @Test
    void redeemsCouponAndIncrementsUsageCount() throws Exception {
        String code = uniqueCode("jesien");
        createCoupon(code, 3, "PL").andExpect(status().isCreated());

        redeem(code, "user-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code.toUpperCase()))
                .andExpect(jsonPath("$.userId").value("user-1"));

        mvc.perform(get("/api/coupons/{code}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usageCount").value(1))
                .andExpect(jsonPath("$.remainingUsages").value(2));
    }

    @Test
    void redemptionIsCaseInsensitive() throws Exception {
        String code = uniqueCode("zima");
        createCoupon(code.toUpperCase(), 1, "PL").andExpect(status().isCreated());

        redeem(code.toLowerCase(), "user-1").andExpect(status().isCreated());
    }

    @Test
    void unknownCouponIsNotFound() throws Exception {
        redeem(uniqueCode("nope"), "user-1")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.reason").value("COUPON_NOT_FOUND"));
    }

    @Test
    void couponCannotBeUsedFromAnotherCountry() throws Exception {
        String code = uniqueCode("germany");
        createCoupon(code, 5, "DE").andExpect(status().isCreated());

        redeem(code, "user-1")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.reason").value("COUNTRY_NOT_ALLOWED"));
    }

    @Test
    void couponCannotBeUsedWhenCountryIsUnknown() throws Exception {
        String code = uniqueCode("unknown");
        createCoupon(code, 5, "PL").andExpect(status().isCreated());
        when(clientCountryResolver.resolve(anyString())).thenReturn(Optional.empty());

        redeem(code, "user-1")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.reason").value("COUNTRY_NOT_ALLOWED"));
    }

    @Test
    void reportsServiceUnavailableWhenGeolocationFails() throws Exception {
        String code = uniqueCode("outage");
        createCoupon(code, 5, "PL").andExpect(status().isCreated());
        when(clientCountryResolver.resolve(anyString())).thenThrow(new ClientCountryUnavailableException("boom"));

        redeem(code, "user-1")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.reason").value("GEOLOCATION_UNAVAILABLE"));
    }

    @Test
    void rejectsRedemptionAfterLimitIsReached() throws Exception {
        String code = uniqueCode("limit");
        createCoupon(code, 1, "PL").andExpect(status().isCreated());

        redeem(code, "user-1").andExpect(status().isCreated());
        redeem(code, "user-2")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("COUPON_LIMIT_REACHED"));

        mvc.perform(get("/api/coupons/{code}", code))
                .andExpect(jsonPath("$.usageCount").value(1));
    }

    @Test
    void sameUserCannotRedeemTwice() throws Exception {
        String code = uniqueCode("once");
        createCoupon(code, 5, "PL").andExpect(status().isCreated());

        redeem(code, "user-1").andExpect(status().isCreated());
        redeem(code, "user-1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("COUPON_ALREADY_REDEEMED"));

        mvc.perform(get("/api/coupons/{code}", code))
                .andExpect(jsonPath("$.usageCount").value(1));
    }

    @Test
    void rejectsRedemptionWithoutUserId() throws Exception {
        mvc.perform(post("/api/coupons/{code}/redemptions", "ANY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.userId").exists());
    }

    // ---- helpers ----

    private ResultActions createCoupon(String code, int maxUsages, String country) throws Exception {
        return mvc.perform(post("/api/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"%s","maxUsages":%d,"country":"%s"}""".formatted(code, maxUsages, country)));
    }

    private ResultActions redeem(String code, String userId) throws Exception {
        return mvc.perform(post("/api/coupons/{code}/redemptions", code)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId":"%s"}""".formatted(userId)));
    }

    private static String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
