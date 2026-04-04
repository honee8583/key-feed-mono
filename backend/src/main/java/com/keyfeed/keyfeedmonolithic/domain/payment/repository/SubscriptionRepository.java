package com.keyfeed.keyfeedmonolithic.domain.payment.repository;

import com.keyfeed.keyfeedmonolithic.domain.payment.entity.Subscription;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByPaymentMethodIdAndStatus(Long methodId, SubscriptionStatus status);

    List<Subscription> findByUserIdAndStatusIn(Long userId, List<SubscriptionStatus> statuses);
}
