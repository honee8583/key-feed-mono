package com.keyfeed.keyfeedmonolithic.domain.notification.dto;

import com.keyfeed.keyfeedmonolithic.domain.notification.entity.Notification;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {
    private Long id;
    private String title;
    private String message;

    public static NotificationResponseDto from(Notification notification) {
        return NotificationResponseDto.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .build();
    }
}
