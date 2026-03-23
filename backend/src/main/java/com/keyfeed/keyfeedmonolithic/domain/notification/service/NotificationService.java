package com.keyfeed.keyfeedmonolithic.domain.notification.service;

import com.keyfeed.keyfeedmonolithic.domain.crawl.dto.CrawledContentDto;
import com.keyfeed.keyfeedmonolithic.domain.notification.dto.NotificationEventDto;
import com.keyfeed.keyfeedmonolithic.domain.notification.dto.NotificationResponseDto;
import com.keyfeed.keyfeedmonolithic.global.response.CommonPageResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationService {

    SseEmitter subscribe(Long userId, String lastEventId);

    void send(NotificationEventDto notificationEvent);

    CommonPageResponse<NotificationResponseDto> getNotificationHistory(Long userId, Long lastId, int size);

    void matchAndSendNotification(CrawledContentDto content);

}
