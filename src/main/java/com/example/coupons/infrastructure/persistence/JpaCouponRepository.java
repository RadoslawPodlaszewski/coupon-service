package com.example.coupons.infrastructure.persistence;

import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.coupons.domain.exception.DuplicateCouponCodeException;
import com.example.coupons.domain.model.Coupon;
import com.example.coupons.domain.model.CouponCode;
import com.example.coupons.domain.repository.CouponRepository;

import jakarta.persistence.EntityManager;

/** Adapter implementing the domain's {@link CouponRepository} port with JPA. */
@Repository
class JpaCouponRepository implements CouponRepository {

    private static final String UNIQUE_CODE_CONSTRAINT = "uq_coupons_code";

    private final CouponJpaRepository jpa;
    private final EntityManager entityManager;

    JpaCouponRepository(CouponJpaRepository jpa, EntityManager entityManager) {
        this.jpa = jpa;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void add(Coupon coupon) {
        try {
            entityManager.persist(CouponJpaEntity.from(coupon));
            // flush now, so a duplicate is reported here and not at some later commit
            entityManager.flush();
        } catch (RuntimeException e) {
            if (isDuplicateCode(e)) {
                throw new DuplicateCouponCodeException(coupon.code());
            }
            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void update(Coupon coupon) {
        // The entity is already managed (loaded in this transaction); changes are flushed on commit.
        CouponJpaEntity entity = jpa.findById(coupon.id().value())
                .orElseThrow(() -> new IllegalStateException("Cannot update missing coupon " + coupon.code().value()));
        entity.updateFrom(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Coupon> findByCode(CouponCode code) {
        return jpa.findByCode(code.value()).map(CouponJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCode(CouponCode code) {
        return jpa.existsByCode(code.value());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Coupon> findByCodeForUpdate(CouponCode code) {
        return jpa.findByCodeForUpdate(code.value()).map(CouponJpaEntity::toDomain);
    }

    private static boolean isDuplicateCode(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && UNIQUE_CODE_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
