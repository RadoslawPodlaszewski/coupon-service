package com.example.coupons.application.port;

import java.util.Optional;

import com.example.coupons.domain.model.CountryCode;

/** Outbound port: where is the client? Implemented by a geolocation adapter. */
public interface ClientCountryResolver {

    /**
     * @return the country, or empty if the address cannot be located (e.g. private range)
     * @throws ClientCountryUnavailableException if the provider failed (timeout, error, rate limit)
     */
    Optional<CountryCode> resolve(String ipAddress);
}
