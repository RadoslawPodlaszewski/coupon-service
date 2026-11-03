package com.example.coupons.interfaces.rest.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Applies the rate limit to redemption attempts only; switch off with {@code coupons.rate-limit.enabled=false}. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "coupons.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
class RateLimitConfiguration implements WebMvcConfigurer {

    private final RateLimitInterceptor interceptor;

    RateLimitConfiguration(RateLimitProperties properties) {
        this.interceptor = new RateLimitInterceptor(new ClientRateLimiter(properties, System::nanoTime));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/coupons/*/redemptions");
    }
}
