package com.keyfeed.keyfeedmonolithic.domain.notification.controller;

import com.keyfeed.keyfeedmonolithic.domain.notification.dto.NotificationResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.notification.service.NotificationService;
import com.keyfeed.keyfeedmonolithic.global.message.SuccessMessage;
import com.keyfeed.keyfeedmonolithic.global.response.CommonPageResponse;
import com.keyfeed.keyfeedmonolithic.global.response.HttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * SSE 연결
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal Long userId,
                                @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {
        log.info("SSE 연결 user: {}, Last-Event-ID: {}", userId, lastEventId);
        return notificationService.subscribe(userId, lastEventId);
    }

    /**
     * 알림 목록 조회
     */
    @GetMapping
    public ResponseEntity<?> getNotifications(@AuthenticationPrincipal Long userId,
                                              @RequestParam(value = "lastId", required = false) Long lastId,
                                              @RequestParam(defaultValue = "20") int size) {
        CommonPageResponse<NotificationResponseDto> notificationHistory = notificationService.getNotificationHistory(userId, lastId, size);
        return ResponseEntity.ok()
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.READ_SUCCESS.getMessage(), notificationHistory));
    }

}
