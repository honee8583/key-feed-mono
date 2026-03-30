package com.keyfeed.keyfeedmonolithic.infra.outbox.entity;

import com.keyfeed.keyfeedmonolithic.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@Entity
@Table(name = "outbox",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_idempotency_key",        // 인덱스 이름 직접 지정
                        columnNames = "idempotency_key"
                )
        },
        indexes = {
                @Index(name = "idx_status_next_retry", columnList = "status, next_retry_at"),
                @Index(name = "idx_aggregate", columnList = "aggregate_type, aggregate_id")
        }

)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "JSON", nullable = false)
    private String payload;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(nullable = false)
    private Long aggregateId;        // contentId

    @Column(nullable = false)
    private String aggregateType;    // "CONTENT"

    @Column(unique = true)
    private String idempotencyKey;   // "CONTENT_CREATED:{contentId}"

    @Builder.Default
    private int retryCount = 0;

    @Builder.Default
    private int maxRetry = 5;

    private LocalDateTime nextRetryAt;

    private LocalDateTime lastTriedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime publishedAt;

    // TODO 생성 팩토리 메서드 메서드명 변경
    public static Outbox create(Long contentId, String payload) {
        return Outbox.builder()
                .eventType("content.created")
                .payload(payload)
                .aggregateId(contentId)
                .aggregateType("CONTENT")
                .idempotencyKey("CONTENT_CREATED:" + contentId)
                .build();
    }

    // 발행 성공
    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    // 발행 실패
    public void markFailed(String errorMessage) {
        this.retryCount++;
        this.lastTriedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.status = OutboxStatus.FAILED;

        // 지수 백오프: 2^retryCount 초 후 재시도
        long delaySeconds = (long) Math.pow(2, this.retryCount);
        this.nextRetryAt = LocalDateTime.now().plusSeconds(delaySeconds);
    }

    public boolean isExhausted() {
        return this.retryCount >= this.maxRetry;
    }
}