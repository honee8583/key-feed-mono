package com.keyfeed.keyfeedmonolithic.domain.payment.entity;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscription")
public class Subscription extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "method_id")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Builder.Default
    private int retryCount = 0;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false, length = 255)
    private String orderName;

    private LocalDateTime startedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime nextBillingAt;
    private LocalDateTime canceledAt;

    public void updatePaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void resume(LocalDateTime nextBillingAt, PaymentMethod paymentMethod) {
        this.status = SubscriptionStatus.ACTIVE;
        this.retryCount = 0;
        this.nextBillingAt = nextBillingAt;
        this.paymentMethod = paymentMethod;
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    public void refund() {
        this.status = SubscriptionStatus.REFUNDED;
        this.canceledAt = LocalDateTime.now();
        this.expiredAt = LocalDateTime.now();
    }

    public void pause() {
        this.status = SubscriptionStatus.PAUSED;
    }

    public void increaseRetryCount() {
        this.retryCount++;
    }

    public void resetRetryCount() {
        this.retryCount = 0;
    }

    public void updateNextBillingAt(LocalDateTime nextBillingAt) {
        this.nextBillingAt = nextBillingAt;
    }
}
