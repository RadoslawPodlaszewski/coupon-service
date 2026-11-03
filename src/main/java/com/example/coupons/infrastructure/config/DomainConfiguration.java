package com.example.coupons.infrastructure.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.coupons.domain.repository.RedemptionRepository;
import com.example.coupons.domain.rule.AlreadyRedeemedRule;
import com.example.coupons.domain.rule.CountryRule;
import com.example.coupons.domain.service.CouponRedeemer;

/** The domain layer is framework-free, so its services are wired here. Rule order is explicit and visible. */
@Configuration(proxyBeanMethods = false)
class DomainConfiguration {

    @Bean
    CouponRedeemer couponRedeemer(RedemptionRepository redemptionRepository) {
        return new CouponRedeemer(List.of(
                new CountryRule(),
                new AlreadyRedeemedRule(redemptionRepository)));
    }
}
