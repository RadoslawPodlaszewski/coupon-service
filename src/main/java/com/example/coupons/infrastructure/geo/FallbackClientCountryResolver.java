package com.example.coupons.infrastructure.geo;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.coupons.application.port.ClientCountryResolver;
import com.example.coupons.application.port.ClientCountryUnavailableException;
import com.example.coupons.domain.model.CountryCode;

/**
 * Chain of responsibility over providers: tries them in order and moves on only when one is
 * <em>unavailable</em>. An empty answer ("cannot locate this address") is final; asking another
 * provider would just spend quota.
 */
class FallbackClientCountryResolver implements ClientCountryResolver {

    private static final Logger log = LoggerFactory.getLogger(FallbackClientCountryResolver.class);

    private final List<ClientCountryResolver> chain;

    FallbackClientCountryResolver(List<ClientCountryResolver> chain) {
        if (chain.isEmpty()) {
            throw new IllegalArgumentException("At least one resolver is required");
        }
        this.chain = List.copyOf(chain);
    }

    @Override
    public Optional<CountryCode> resolve(String ipAddress) {
        ClientCountryUnavailableException last = null;
        for (ClientCountryResolver resolver : chain) {
            try {
                return resolver.resolve(ipAddress);
            } catch (ClientCountryUnavailableException e) {
                log.warn("Geolocation provider unavailable, trying the next one: {}", e.getMessage());
                last = e;
            }
        }
        throw new ClientCountryUnavailableException("All geolocation providers failed", last);
    }
}
