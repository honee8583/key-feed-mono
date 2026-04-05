package com.keyfeed.keyfeedmonolithic.domain.payment.entity;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_history")
@EntityListeners(AuditingEntityListener.class)
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "method_id")
    private PaymentMethod paymentMethod;

    @Column(nullable = false, length = 100, unique = true)
    private String orderId;

    @Column(length = 255)
    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PaymentMethodType methodType;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentHistoryStatus status;

    @Column(length = 255)
    private String orderName;

    @Column(columnDefinition = "text")
    private String failReason;

    private LocalDateTime approvedAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void markDone(String paymentKey, LocalDateTime approvedAt) {
        this.status = PaymentHistoryStatus.DONE;
        this.paymentKey = paymentKey;
        this.approvedAt = approvedAt;
    }

    public void markFailed(String failReason) {
        this.status = PaymentHistoryStatus.FAILED;
        this.failReason = failReason;
    }

    public void markCanceled() {
        this.status = PaymentHistoryStatus.CANCELED;
    }

    public void linkSubscription(Subscription subscription) {
        this.subscription = subscription;
    }
}
