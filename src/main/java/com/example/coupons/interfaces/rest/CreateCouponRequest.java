package com.example.coupons.interfaces.rest;

import com.example.coupons.domain.model.CountryCode;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

record CreateCouponRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,32}$",
                message = "must be 3-32 characters: letters, digits, '-' or '_'")
        String code,

        @NotNull @Min(1) Integer maxUsages,

        @NotBlank String country) {

    @JsonIgnore
    @AssertTrue(message = "country must be a valid ISO 3166-1 alpha-2 code, e.g. PL")
    public boolean isCountryValid() {
        // blank/null is reported by @NotBlank, so no duplicate error here
        return country == null || country.isBlank() || CountryCode.isValid(country);
    }
}
