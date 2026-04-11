package com.keyfeed.keyfeedmonolithic.domain.payment.writer;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentHistory;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentHistoryStatus;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentMethod;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.Subscription;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.PaymentHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PaymentHistoryWriter {

    private final PaymentHistoryRepository paymentHistoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentHistory saveReady(User user, PaymentMethod paymentMethod,
                                    Subscription subscription, String orderId,
                                    String orderName, int amount) {
        return paymentHistoryRepository.save(
                PaymentHistory.builder()
                        .user(user)
                        .paymentMethod(paymentMethod)
                        .subscription(subscription)
                        .orderId(orderId)
                        .orderName(orderName)
                        .amount(amount)
                        .methodType(paymentMethod.getMethodType())
                        .status(PaymentHistoryStatus.READY)
                        .build()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDone(PaymentHistory history, String paymentKey, LocalDateTime approvedAt) {
        history.markDone(paymentKey, approvedAt);
        paymentHistoryRepository.save(history);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateFailed(PaymentHistory history, String reason) {
        history.markFailed(reason);
        paymentHistoryRepository.save(history);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void linkSubscription(PaymentHistory history, Subscription subscription) {
        history.linkSubscription(subscription);
        paymentHistoryRepository.save(history);
    }
}
