package com.keyfeed.keyfeedmonolithic.domain.payment.repository;

import com.keyfeed.keyfeedmonolithic.domain.payment.entity.Subscription;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByPaymentMethodIdAndStatus(Long methodId, SubscriptionStatus status);

    List<Subscription> findByUserIdAndStatusIn(Long userId, List<SubscriptionStatus> statuses);

    Optional<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    boolean existsByUserIdAndStatus(Long userId, SubscriptionStatus status);

    Optional<Subscription> findTopByUserIdAndStatusInOrderByCreatedAtDesc(Long userId, List<SubscriptionStatus> statuses);

    List<Subscription> findByStatusAndNextBillingAtLessThanEqual(SubscriptionStatus status, LocalDateTime dateTime);

    List<Subscription> findByStatusAndExpiredAtLessThanEqual(SubscriptionStatus status, LocalDateTime dateTime);

    boolean existsByUserIdAndStatusIn(Long userId, List<SubscriptionStatus> statuses);
}
