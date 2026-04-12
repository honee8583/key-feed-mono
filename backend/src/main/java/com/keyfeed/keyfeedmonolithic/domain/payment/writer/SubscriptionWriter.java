package com.keyfeed.keyfeedmonolithic.domain.payment.writer;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentMethod;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.Subscription;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.SubscriptionConstants;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.SubscriptionStatus;

import java.time.LocalDateTime;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.ActiveSubscriptionAlreadyExistsException;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SubscriptionWriter {

    private final SubscriptionRepository subscriptionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Subscription savePending(User user, PaymentMethod paymentMethod) {
        try {
            return subscriptionRepository.save(
                    Subscription.builder()
                            .user(user)
                            .paymentMethod(paymentMethod)
                            .status(SubscriptionStatus.PENDING)
                            .price(SubscriptionConstants.SUBSCRIPTION_PRICE)
                            .orderName(SubscriptionConstants.SUBSCRIPTION_ORDER_NAME)
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateResume(Subscription subscription, LocalDateTime nextBillingAt, PaymentMethod paymentMethod) {
        subscription.resume(nextBillingAt, paymentMethod);
        subscriptionRepository.save(subscription);
    }
}
