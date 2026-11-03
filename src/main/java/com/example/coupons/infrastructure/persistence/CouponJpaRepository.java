package com.example.coupons.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

interface CouponJpaRepository extends JpaRepository<CouponJpaEntity, UUID> {

    Optional<CouponJpaEntity> findByCode(String code);

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CouponJpaEntity c where c.code = :code")
    Optional<CouponJpaEntity> findByCodeForUpdate(@Param("code") String code);
}
