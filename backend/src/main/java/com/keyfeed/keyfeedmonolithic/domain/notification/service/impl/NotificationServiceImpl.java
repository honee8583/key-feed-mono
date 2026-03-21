package com.keyfeed.keyfeedmonolithic.domain.notification.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.notification.dto.NotificationEventDto;
import com.keyfeed.keyfeedmonolithic.domain.notification.dto.NotificationResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.notification.entity.Notification;
import com.keyfeed.keyfeedmonolithic.domain.notification.repository.NotificationRepository;
import com.keyfeed.keyfeedmonolithic.domain.notification.repository.SseEmitterRepository;
import com.keyfeed.keyfeedmonolithic.domain.notification.service.NotificationService;
import com.keyfeed.keyfeedmonolithic.global.response.CommonPageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SseEmitterRepository sseEmitterRepository;
    private final NotificationRepository notificationRepository;

    // 연결 지속 시간 (1시간)
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    private static final String EVENT_NAME_NOTIFICATION = "notification";
    private static final String EVENT_DATA_CONNECTED = "connected";

    // SSE 구독 (연결)
    @Override
    public SseEmitter subscribe(Long userId, String lastEventId) {
        // 고유 Emitter ID 생성 (userId + 시간)
        String emitterId = userId + "_" + System.currentTimeMillis();

        // Emitter 생성 및 저장
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        sseEmitterRepository.save(emitterId, emitter);

        // 완료/타임아웃 시 삭제
        emitter.onCompletion(() -> sseEmitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> sseEmitterRepository.deleteById(emitterId));

        // 503 에러 방지를 위한 더미 데이터 전송
        sendToClient(emitter, emitterId, 0L, EVENT_NAME_NOTIFICATION, EVENT_DATA_CONNECTED);

        // 유실된 데이터 재전송
        if (!lastEventId.isEmpty()) {
            try {
                // 클라이언트가 마지막으로 수신한 ID보다 이후에 생성된 알림 조회
                Long lastId = Long.parseLong(lastEventId);

                log.info("이전 알림 내역을 조회합니다.");
                log.info("Last-Event-ID: {}", lastEventId);

                List<Notification> missedNotifications = notificationRepository.findAllByUserIdAndIdGreaterThan(userId, lastId);

                for (Notification notification : missedNotifications) {
                    NotificationResponseDto notificationResponse = NotificationResponseDto.builder()
                            .id(notification.getId())
                            .title(notification.getTitle())
                            .message(notification.getMessage())
                            .build();
                    sendToClient(emitter, emitterId, notification.getId(), EVENT_NAME_NOTIFICATION, notificationResponse);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid Last-Event-ID format: {}", lastEventId);
            }
        }

        return emitter;
    }

    // 알림 전송
    @Override
    @Transactional
    public void send(NotificationEventDto notificationEvent) {
        // JPA로 DB 저장 (ID 자동 생성)
        Notification saved = notificationRepository.save(Notification.builder()
                .userId(notificationEvent.getUserId())
                .title(notificationEvent.getTitle())
                .message(notificationEvent.getMessage())
                .url(notificationEvent.getOriginalUrl())
                .build());

        // 사용자의 모든 연결된 Emitter 조회
        Map<String, SseEmitter> emitters = sseEmitterRepository.findAllEmitterStartWithByUserId(String.valueOf(notificationEvent.getUserId()));

        log.info("emitter 개수 {}", emitters.size());

        NotificationResponseDto notificationResponse = NotificationResponseDto.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .message(saved.getMessage())
                .build();

        // 모든 Emitter에 알림 전송 (event ID = DB generated ID)
        emitters.forEach((key, emitter) -> {
            sendToClient(emitter, key, saved.getId(), EVENT_NAME_NOTIFICATION, notificationResponse);
        });
    }

    // 알림 목록 조회
    @Override
    @Transactional(readOnly = true)
    public CommonPageResponse<NotificationResponseDto> getNotificationHistory(Long userId, Long lastId, int size) {

        int safeSize = Math.min(size, 50);

        int fetchSize = safeSize + 1;
        Pageable pageable = PageRequest.of(0, fetchSize);

        List<Notification> notifications;
        if (lastId == null) {
            notifications = notificationRepository.findFirstPage(userId, pageable);
        } else {
            notifications = notificationRepository.findNextPage(userId, lastId, pageable);
        }

        boolean hasNext = false;
        if (notifications.size() > safeSize) {
            hasNext = true;
            notifications.remove(safeSize);
        }

        Long nextCursorId = null;
        if (!notifications.isEmpty()) {
            nextCursorId = notifications.get(notifications.size() - 1).getId();
        }

        List<NotificationResponseDto> notificationResponseDtos = notifications.stream()
                .map(NotificationResponseDto::from)
                .toList();

        return CommonPageResponse.<NotificationResponseDto>builder()
                .content(notificationResponseDtos)
                .nextCursorId(nextCursorId)
                .hasNext(hasNext)
                .build();
    }

    // 실제 클라이언트로 데이터 전송
    private void sendToClient(SseEmitter emitter, String emitterId, Long notificationId, String name, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(notificationId))
                    .name(name)
                    .data(data));
        } catch (IOException e) {
            sseEmitterRepository.deleteById(emitterId);
            log.error("SSE 연결 오류 발생 (Emitter 삭제): {}", e.getMessage());
        }
    }

}
