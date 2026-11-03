package com.example.coupons.infrastructure.geo;

import java.util.Optional;

import com.example.coupons.application.port.ClientCountryResolver;
import com.example.coupons.application.port.ClientCountryUnavailableException;
import com.example.coupons.domain.model.CountryCode;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

/**
 * Circuit breaker around one provider. After enough failures it "opens" and rejects calls immediately,
 * so request threads do not wait for timeouts and a struggling provider is not hammered.
 * After a cool-down it lets a few probe calls through to see if the provider recovered.
 * <p>
 * Only {@link ClientCountryUnavailableException} counts as a failure; "cannot locate this address"
 * (an empty result) is a valid answer, not an outage.
 */
class CircuitBreakingClientCountryResolver implements ClientCountryResolver {

    private final String name;
    private final ClientCountryResolver delegate;
    private final CircuitBreaker circuitBreaker;

    CircuitBreakingClientCountryResolver(String name, ClientCountryResolver delegate, CircuitBreakerConfig config) {
        this.name = name;
        this.delegate = delegate;
        this.circuitBreaker = CircuitBreaker.of(name, config);
    }

    @Override
    public Optional<CountryCode> resolve(String ipAddress) {
        try {
            return circuitBreaker.executeSupplier(() -> delegate.resolve(ipAddress));
        } catch (CallNotPermittedException e) {
            throw new ClientCountryUnavailableException(
                    "Geolocation provider '%s': circuit breaker is open".formatted(name), e);
        }
    }
}
