package com.example.coupons.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.example.coupons.domain.model.CountryCode;
import com.example.coupons.domain.model.Coupon;
import com.example.coupons.domain.model.CouponCode;
import com.example.coupons.domain.model.CouponId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Persistence model; deliberately separate from the domain {@link Coupon}. */
@Entity
@Table(name = "coupons")
class CouponJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String code;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "max_usages", nullable = false)
    private int maxUsages;

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    protected CouponJpaEntity() {
        // for JPA
    }

    static CouponJpaEntity from(Coupon coupon) {
        CouponJpaEntity entity = new CouponJpaEntity();
        entity.id = coupon.id().value();
        entity.code = coupon.code().value();
        entity.createdAt = coupon.createdAt();
        entity.maxUsages = coupon.maxUsages();
        entity.usageCount = coupon.usageCount();
        entity.countryCode = coupon.country().value();
        return entity;
    }

    /** The only state that changes after creation. */
    void updateFrom(Coupon coupon) {
        this.usageCount = coupon.usageCount();
    }

    Coupon toDomain() {
        return Coupon.restore(
                new CouponId(id),
                new CouponCode(code),
                createdAt,
                maxUsages,
                usageCount,
                new CountryCode(countryCode));
    }
}
