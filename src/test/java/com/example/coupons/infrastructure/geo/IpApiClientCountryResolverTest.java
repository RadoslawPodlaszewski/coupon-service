package com.example.coupons.infrastructure.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.coupons.application.port.ClientCountryUnavailableException;
import com.example.coupons.domain.model.CountryCode;

class IpApiClientCountryResolverTest {

    private MockRestServiceServer server;
    private IpApiClientCountryResolver resolver;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ip-api.test");
        server = MockRestServiceServer.bindTo(builder).build();
        resolver = new IpApiClientCountryResolver(builder.build());
    }

    @Test
    void returnsCountry() {
        server.expect(requestTo("http://ip-api.test/json/8.8.8.8?fields=status,countryCode"))
                .andRespond(withSuccess("{\"status\":\"success\",\"countryCode\":\"us\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(resolver.resolve("8.8.8.8")).contains(new CountryCode("US"));
    }

    @Test
    void returnsEmptyWhenProviderCannotLocateTheAddress() {
        server.expect(requestTo("http://ip-api.test/json/127.0.0.1?fields=status,countryCode"))
                .andRespond(withSuccess("{\"status\":\"fail\",\"message\":\"reserved range\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(resolver.resolve("127.0.0.1")).isEmpty();
    }

    @Test
    void signalsUnavailabilityOnProviderError() {
        server.expect(requestTo("http://ip-api.test/json/8.8.8.8?fields=status,countryCode"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> resolver.resolve("8.8.8.8"))
                .isInstanceOf(ClientCountryUnavailableException.class);
    }
}
