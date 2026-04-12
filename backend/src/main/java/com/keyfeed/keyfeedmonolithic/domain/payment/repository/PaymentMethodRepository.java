package com.keyfeed.keyfeedmonolithic.domain.payment.repository;

import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByUserIdAndIsActiveTrue(Long userId);

    Optional<PaymentMethod> findByIdAndIsActiveTrue(Long id);

    Optional<PaymentMethod> findByIdAndUserIdAndIsActiveTrue(Long id, Long userId);

    boolean existsByUserIdAndDisplayNumberAndIsActiveTrue(Long userId, String displayNumber);

    Optional<PaymentMethod> findByUserIdAndIsDefaultTrueAndIsActiveTrue(Long userId);

    Optional<PaymentMethod> findTopByUserIdAndIsActiveTrueOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsActiveTrue(Long userId);
}
