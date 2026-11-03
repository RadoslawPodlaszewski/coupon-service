package com.example.coupons.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** ISO 3166-1 alpha-2 country code. Invalid values cannot be constructed. */
public record CountryCode(String value) {

    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    public CountryCode {
        Objects.requireNonNull(value, "value");
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!ISO_COUNTRIES.contains(value)) {
            throw new IllegalArgumentException("Not an ISO 3166-1 alpha-2 country code: " + value);
        }
    }

    public static boolean isValid(String candidate) {
        return candidate != null && ISO_COUNTRIES.contains(candidate.strip().toUpperCase(Locale.ROOT));
    }
}
