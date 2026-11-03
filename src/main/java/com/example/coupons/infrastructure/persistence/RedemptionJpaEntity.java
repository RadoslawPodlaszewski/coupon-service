package com.example.coupons.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.example.coupons.domain.model.Redemption;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "coupon_usages")
class RedemptionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private UUID couponId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "used_at", nullable = false)
    private Instant usedAt;

    protected RedemptionJpaEntity() {
        // for JPA
    }

    static RedemptionJpaEntity from(Redemption redemption) {
        RedemptionJpaEntity entity = new RedemptionJpaEntity();
        entity.couponId = redemption.couponId().value();
        entity.userId = redemption.userId().value();
        entity.usedAt = redemption.redeemedAt();
        return entity;
    }
}
