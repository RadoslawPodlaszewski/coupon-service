package com.example.coupons.infrastructure.geo;

import java.time.Duration;
import java.util.Optional;

import com.example.coupons.application.port.ClientCountryResolver;
import com.example.coupons.domain.model.CountryCode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Decorator caching lookups per IP: protects the provider's rate limit and removes latency.
 * Failures are not cached (an exception from the delegate stores nothing).
 */
class CachingClientCountryResolver implements ClientCountryResolver {

    private final ClientCountryResolver delegate;
    private final Cache<String, Optional<CountryCode>> cache;

    CachingClientCountryResolver(ClientCountryResolver delegate, Duration ttl, long maxSize) {
        this.delegate = delegate;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maxSize)
                .build();
    }

    @Override
    public Optional<CountryCode> resolve(String ipAddress) {
        return cache.get(ipAddress, delegate::resolve);
    }
}
