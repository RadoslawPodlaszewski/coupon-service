package com.example.coupons.infrastructure.geo;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.example.coupons.application.port.ClientCountryResolver;
import com.example.coupons.application.port.ClientCountryUnavailableException;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

/**
 * Wiring, outermost first:
 * cache -> fallback chain -> [circuit breaker -> primary provider, circuit breaker -> secondary provider].
 * The cache is outermost so cache hits cost nothing, and failures (exceptions) are never cached.
 */
@Configuration(proxyBeanMethods = false)
class GeoConfiguration {

    @Bean
    ClientCountryResolver clientCountryResolver(GeoProperties properties, RestClient.Builder restClientBuilder) {
        CircuitBreakerConfig breakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.failureRateThreshold())
                .slidingWindowSize(properties.slidingWindowSize())
                .minimumNumberOfCalls(properties.minimumCalls())
                .waitDurationInOpenState(properties.openStateDuration())
                .recordExceptions(ClientCountryUnavailableException.class)
                .build();

        ClientCountryResolver primary = new CircuitBreakingClientCountryResolver(
                "ip-api",
                new IpApiClientCountryResolver(restClient(restClientBuilder, properties.baseUrl(), properties)),
                breakerConfig);
        ClientCountryResolver secondary = new CircuitBreakingClientCountryResolver(
                "ipapi-co",
                new IpApiCoClientCountryResolver(restClient(restClientBuilder, properties.fallbackBaseUrl(), properties)),
                breakerConfig);

        return new CachingClientCountryResolver(
                new FallbackClientCountryResolver(List.of(primary, secondary)),
                properties.cacheTtl(),
                properties.cacheMaxSize());
    }

    private static RestClient restClient(RestClient.Builder builder, String baseUrl, GeoProperties properties) {
        // Explicit timeouts: a third-party service must never hang request threads.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.connectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.readTimeout().toMillis());
        return builder.clone().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
