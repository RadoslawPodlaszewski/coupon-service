package com.example.coupons.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RedemptionJpaRepository extends JpaRepository<RedemptionJpaEntity, Long> {

    boolean existsByCouponIdAndUserId(UUID couponId, String userId);
}
