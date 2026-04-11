package com.keyfeed.keyfeedmonolithic.domain.payment.writer;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentMethod;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.Subscription;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.SubscriptionStatus;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.ActiveSubscriptionAlreadyExistsException;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SubscriptionWriter {

    private static final int SUBSCRIPTION_PRICE = 100;
    private static final String SUBSCRIPTION_ORDER_NAME = "프리미엄 구독 1개월";

    private final SubscriptionRepository subscriptionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Subscription savePending(User user, PaymentMethod paymentMethod) {
        try {
            return subscriptionRepository.save(
                    Subscription.builder()
                            .user(user)
                            .paymentMethod(paymentMethod)
                            .status(SubscriptionStatus.PENDING)
                            .price(SUBSCRIPTION_PRICE)
                            .orderName(SUBSCRIPTION_ORDER_NAME)
                            .retryCount(0)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            throw new ActiveSubscriptionAlreadyExistsException();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateActive(Subscription subscription) {
        LocalDateTime now = LocalDateTime.now();
        subscription.activate(now);
        subscriptionRepository.save(subscription);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCanceled(Subscription subscription) {
        subscription.cancel();
        subscriptionRepository.save(subscription);
    }
}
