package com.example.coupons.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.coupons.domain.model.CouponId;
import com.example.coupons.domain.model.Redemption;
import com.example.coupons.domain.model.UserId;
import com.example.coupons.domain.repository.RedemptionRepository;

@Repository
class JpaRedemptionRepository implements RedemptionRepository {

    private final RedemptionJpaRepository jpa;

    JpaRedemptionRepository(RedemptionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean existsByCouponIdAndUserId(CouponId couponId, UserId userId) {
        return jpa.existsByCouponIdAndUserId(couponId.value(), userId.value());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(Redemption redemption) {
        jpa.save(RedemptionJpaEntity.from(redemption));
    }
}
