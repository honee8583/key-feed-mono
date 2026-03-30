package com.keyfeed.keyfeedmonolithic.domain.notification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

// content.created 역직렬화용
@Getter
@NoArgsConstructor
public class ContentEventPayload {
    private Long contentId;
    private Long sourceId;
    private String title;
    private String summary;
    private String originalUrl;
    private String createdAt;
}
