package com.example.coupons.infrastructure.idempotency;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "coupons.idempotency")
public record IdempotencyProperties(@NotNull Duration inProgressTimeout, @NotNull Duration retention) {
}
