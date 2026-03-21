package com.keyfeed.keyfeedmonolithic.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventDto {
    private Long userId;
    private String title;
    private String message;
    private String originalUrl;
}
