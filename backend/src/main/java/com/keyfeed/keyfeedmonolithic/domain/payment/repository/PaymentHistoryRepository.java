package com.keyfeed.keyfeedmonolithic.domain.payment.repository;

import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentHistory;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentHistoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {

    Optional<PaymentHistory> findByOrderId(String orderId);

    Optional<PaymentHistory> findTopBySubscriptionIdAndStatusOrderByCreatedAtDesc(Long subscriptionId, PaymentHistoryStatus status);
}
