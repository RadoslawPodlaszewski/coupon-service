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

class IpApiCoClientCountryResolverTest {

    private MockRestServiceServer server;
    private IpApiCoClientCountryResolver resolver;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ipapi.test");
        server = MockRestServiceServer.bindTo(builder).build();
        resolver = new IpApiCoClientCountryResolver(builder.build());
    }

    @Test
    void returnsTheCountryFromAPlainTextBody() {
        server.expect(requestTo("http://ipapi.test/8.8.8.8/country/"))
                .andRespond(withSuccess("US\n", MediaType.TEXT_PLAIN));

        assertThat(resolver.resolve("8.8.8.8")).contains(new CountryCode("US"));
    }

    @Test
    void returnsEmptyWhenTheBodyIsNotACountryCode() {
        server.expect(requestTo("http://ipapi.test/10.0.0.1/country/"))
                .andRespond(withSuccess("Undefined", MediaType.TEXT_PLAIN));

        assertThat(resolver.resolve("10.0.0.1")).isEmpty();
    }

    @Test
    void signalsUnavailabilityOnProviderError() {
        server.expect(requestTo("http://ipapi.test/8.8.8.8/country/")).andRespond(withServerError());

        assertThatThrownBy(() -> resolver.resolve("8.8.8.8"))
                .isInstanceOf(ClientCountryUnavailableException.class);
    }
}
