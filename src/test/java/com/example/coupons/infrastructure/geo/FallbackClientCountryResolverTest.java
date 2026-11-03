package com.example.coupons.infrastructure.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.coupons.application.port.ClientCountryResolver;
import com.example.coupons.application.port.ClientCountryUnavailableException;
import com.example.coupons.domain.model.CountryCode;

class FallbackClientCountryResolverTest {

    private static final String IP = "8.8.8.8";

    private final ClientCountryResolver primary = mock(ClientCountryResolver.class);
    private final ClientCountryResolver secondary = mock(ClientCountryResolver.class);
    private final FallbackClientCountryResolver resolver = new FallbackClientCountryResolver(List.of(primary, secondary));

    @Test
    void usesThePrimaryWhenItWorks() {
        when(primary.resolve(IP)).thenReturn(Optional.of(new CountryCode("US")));

        assertThat(resolver.resolve(IP)).contains(new CountryCode("US"));
        verifyNoInteractions(secondary);
    }

    @Test
    void fallsBackWhenThePrimaryIsUnavailable() {
        when(primary.resolve(IP)).thenThrow(new ClientCountryUnavailableException("down"));
        when(secondary.resolve(IP)).thenReturn(Optional.of(new CountryCode("PL")));

        assertThat(resolver.resolve(IP)).contains(new CountryCode("PL"));
    }

    @Test
    void anEmptyAnswerIsFinalAndDoesNotTriggerTheFallback() {
        when(primary.resolve(IP)).thenReturn(Optional.empty());

        assertThat(resolver.resolve(IP)).isEmpty();
        verifyNoInteractions(secondary);
    }

    @Test
    void failsWhenEveryProviderIsUnavailable() {
        when(primary.resolve(IP)).thenThrow(new ClientCountryUnavailableException("down 1"));
        when(secondary.resolve(IP)).thenThrow(new ClientCountryUnavailableException("down 2"));

        assertThatThrownBy(() -> resolver.resolve(IP))
                .isInstanceOf(ClientCountryUnavailableException.class)
                .hasMessageContaining("All geolocation providers failed");
        verify(secondary).resolve(IP);
    }
}
