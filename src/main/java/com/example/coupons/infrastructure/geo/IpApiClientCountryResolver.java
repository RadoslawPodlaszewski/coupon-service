package com.example.coupons.infrastructure.geo;

import java.util.Optional;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.coupons.application.port.ClientCountryResolver;
import com.example.coupons.application.port.ClientCountryUnavailableException;
import com.example.coupons.domain.model.CountryCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** {@link ClientCountryResolver} backed by the free ip-api.com service. */
class IpApiClientCountryResolver implements ClientCountryResolver {

    private final RestClient restClient;

    IpApiClientCountryResolver(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<CountryCode> resolve(String ipAddress) {
        try {
            IpApiResponse response = restClient.get()
                    .uri("/json/{ip}?fields=status,countryCode", ipAddress)
                    .retrieve()
                    .body(IpApiResponse.class);

            if (response == null) {
                throw new ClientCountryUnavailableException("Empty response from geolocation provider");
            }
            if (!"success".equals(response.status()) || !CountryCode.isValid(response.countryCode())) {
                return Optional.empty();
            }
            return Optional.of(new CountryCode(response.countryCode()));
        } catch (RestClientException e) {
            throw new ClientCountryUnavailableException("Geolocation provider call failed", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record IpApiResponse(String status, String countryCode) {
    }
}
