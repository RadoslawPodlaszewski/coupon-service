package com.example.coupons.interfaces.rest.ratelimit;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "coupons.rate-limit")
record RateLimitProperties(
        @Positive long capacity,
        @Positive long refillTokens,
        @NotNull Duration refillPeriod,
        @Positive long maxTrackedClients) {
}
