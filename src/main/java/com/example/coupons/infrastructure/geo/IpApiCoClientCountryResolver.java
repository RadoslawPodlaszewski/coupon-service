package com.example.coupons.infrastructure.geo;

import java.util.Optional;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.coupons.application.port.ClientCountryResolver;
import com.example.coupons.application.port.ClientCountryUnavailableException;
import com.example.coupons.domain.model.CountryCode;

/** Secondary provider (ipapi.co), asked only when the primary one is failing. Returns a plain-text country code. */
class IpApiCoClientCountryResolver implements ClientCountryResolver {

    private final RestClient restClient;

    IpApiCoClientCountryResolver(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<CountryCode> resolve(String ipAddress) {
        try {
            String body = restClient.get()
                    .uri("/{ip}/country/", ipAddress)
                    .retrieve()
                    .body(String.class);

            if (body == null) {
                throw new ClientCountryUnavailableException("Empty response from fallback geolocation provider");
            }
            String candidate = body.strip();
            return CountryCode.isValid(candidate) ? Optional.of(new CountryCode(candidate)) : Optional.empty();
        } catch (RestClientException e) {
            throw new ClientCountryUnavailableException("Fallback geolocation provider call failed", e);
        }
    }
}
