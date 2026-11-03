package com.example.coupons.infrastructure.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.coupons.application.port.ClientCountryResolver;
import com.example.coupons.application.port.ClientCountryUnavailableException;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

class CircuitBreakingClientCountryResolverTest {

    private final ClientCountryResolver delegate = mock(ClientCountryResolver.class);
    private final CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .slidingWindowSize(4)
            .minimumNumberOfCalls(4)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMinutes(1))
            .recordExceptions(ClientCountryUnavailableException.class)
            .build();
    private final CircuitBreakingClientCountryResolver resolver =
            new CircuitBreakingClientCountryResolver("test", delegate, config);

    @Test
    void opensAfterRepeatedFailuresAndStopsCallingTheProvider() {
        when(delegate.resolve(anyString())).thenThrow(new ClientCountryUnavailableException("down"));

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> resolver.resolve("1.1.1.1"))
                    .isInstanceOf(ClientCountryUnavailableException.class);
        }
        // the circuit is now open: rejected without touching the provider
        assertThatThrownBy(() -> resolver.resolve("1.1.1.1"))
                .isInstanceOf(ClientCountryUnavailableException.class)
                .hasMessageContaining("circuit breaker is open");

        verify(delegate, times(4)).resolve(anyString());
    }

    @Test
    void anEmptyAnswerIsNotAFailure() {
        when(delegate.resolve(anyString())).thenReturn(Optional.empty());

        for (int i = 0; i < 10; i++) {
            assertThat(resolver.resolve("10.0.0.1")).isEmpty();
        }

        verify(delegate, times(10)).resolve(anyString());   // never short-circuited
    }
}
