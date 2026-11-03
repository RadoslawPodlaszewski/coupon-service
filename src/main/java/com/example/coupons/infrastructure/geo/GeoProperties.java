package com.example.coupons.infrastructure.geo;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "coupons.geo")
record GeoProperties(
        @NotBlank String baseUrl,
        @NotBlank String fallbackBaseUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @NotNull Duration cacheTtl,
        @Positive long cacheMaxSize,
        @Min(1) @Max(100) float failureRateThreshold,
        @Positive int slidingWindowSize,
        @Positive int minimumCalls,
        @NotNull Duration openStateDuration) {
}
