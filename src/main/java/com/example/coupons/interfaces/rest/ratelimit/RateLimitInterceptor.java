package com.example.coupons.interfaces.rest.ratelimit;

import java.time.Duration;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Rejects redemption attempts from a client that exceeded its rate; handled as 429 by the exception handler. */
class RateLimitInterceptor implements HandlerInterceptor {

    private final ClientRateLimiter limiter;

    RateLimitInterceptor(ClientRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equals(request.getMethod())) {
            return true;
        }
        // getRemoteAddr() reflects X-Forwarded-For only for trusted proxies, so the key cannot be spoofed.
        Duration wait = limiter.tryAcquire(request.getRemoteAddr());
        if (!wait.isZero()) {
            throw new RateLimitExceededException(wait);
        }
        return true;
    }
}
