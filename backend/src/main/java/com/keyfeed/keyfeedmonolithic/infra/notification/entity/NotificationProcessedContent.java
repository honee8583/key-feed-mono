package com.keyfeed.keyfeedmonolithic.infra.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_processed_contents")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationProcessedContent {

    @Id
    private Long contentId;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime processedAt = LocalDateTime.now();

    public static NotificationProcessedContent of(Long contentId) {
        return NotificationProcessedContent.builder()
                .contentId(contentId)
                .build();
    }
}
