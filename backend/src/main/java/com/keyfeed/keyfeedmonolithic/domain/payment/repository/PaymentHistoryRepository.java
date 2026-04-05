package com.keyfeed.keyfeedmonolithic.domain.payment.repository;

import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentHistory;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentHistoryStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {

    Optional<PaymentHistory> findByOrderId(String orderId);

    Optional<PaymentHistory> findTopBySubscriptionIdAndStatusOrderByCreatedAtDesc(Long subscriptionId, PaymentHistoryStatus status);

    @Query("SELECT ph FROM PaymentHistory ph WHERE ph.user.id = :userId AND ph.status != :excludeStatus AND (:cursorId IS NULL OR ph.id < :cursorId) ORDER BY ph.id DESC")
    List<PaymentHistory> findByUserIdWithCursor(@Param("userId") Long userId,
                                                @Param("excludeStatus") PaymentHistoryStatus excludeStatus,
                                                @Param("cursorId") Long cursorId,
                                                Pageable pageable);

    @Query("SELECT ph FROM PaymentHistory ph WHERE ph.user.id = :userId AND ph.status = :status AND (:cursorId IS NULL OR ph.id < :cursorId) ORDER BY ph.id DESC")
    List<PaymentHistory> findByUserIdAndStatusWithCursor(@Param("userId") Long userId,
                                                         @Param("status") PaymentHistoryStatus status,
                                                         @Param("cursorId") Long cursorId,
                                                         Pageable pageable);
}
