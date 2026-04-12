package com.keyfeed.keyfeedmonolithic.infra.notification.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keyfeed.keyfeedmonolithic.domain.notification.dto.ContentEventPayload;
import com.keyfeed.keyfeedmonolithic.domain.notification.repository.NotificationJdbcRepository;
import com.keyfeed.keyfeedmonolithic.domain.source.repository.UserSourceRepository;
import com.keyfeed.keyfeedmonolithic.infra.notification.entity.NotificationProcessedContent;
import com.keyfeed.keyfeedmonolithic.infra.notification.repository.NotificationProcessedContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private static final String QUEUE_KEY = "queue:content.created";
    private static final String KEYWORD_KEY_PREFIX = "keyword:users:";
    private static final int CHUNK_SIZE = 1000;

    private final UserSourceRepository userSourceRepository;
    private final NotificationProcessedContentRepository processRepository;
    private final NotificationJdbcRepository notificationJdbcRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 500, scheduler = "notificationConsumerScheduler")
    public void consume() {
        String payload = redisTemplate.opsForList().rightPop(QUEUE_KEY);
        if (payload == null) {
            return;
        }

        process(payload);

        log.info("[NotificationConsumer] 알림 저장 완료.");
    }

    private void process(String payload) {
        ContentEventPayload content = deserialize(payload);

        if (content == null) {
            log.error("[NotificationConsumer] 역직렬화 실패 - payload: {}", payload);
            return;
        }

        // 멱등성 체크
        if (processRepository.existsByContentId(content.getContentId())) {
            log.info("[NotificationConsumer] 중복 이벤트 스킵 - contentId: {}", content.getContentId());
            return;
        }

        long startTime = System.nanoTime();

        // ① 블로그 구독자 조회
        Set<Long> sourceSubscribers = new HashSet<>(
                userSourceRepository.findUserIdsBySourceId(content.getSourceId())
        );
        if (sourceSubscribers.isEmpty()) {
            log.info("[NotificationConsumer] 블로그 구독자 없음 - sourceId: {}", content.getSourceId());
            processRepository.save(NotificationProcessedContent.of(content.getContentId()));
            return;
        }

        // ② 키워드 추출
        Set<String> keywords = extractKeywords(content.getTitle(), content.getSummary());
        if (keywords.isEmpty()) {
            log.info("[NotificationConsumer] 키워드 없음 - contentId: {}", content.getContentId());
            processRepository.save(NotificationProcessedContent.of(content.getContentId()));
            return;
        }

        // ③ 키워드별 순차 SSCAN + 교집합 필터링 + 매칭 키워드 수집
        Map<Long, Set<String>> userMatchedKeywords = collectMatchedKeywords(sourceSubscribers, keywords);
        if (userMatchedKeywords.isEmpty()) {
            log.info("[NotificationConsumer] 최종 알림 대상 없음 - contentId: {}", content.getContentId());
            processRepository.save(NotificationProcessedContent.of(content.getContentId()));
            return;
        }

        // ④ 배치 INSERT
        saveNotificationsInChunks(userMatchedKeywords, content);

        // ⑤ 처리 완료 기록
        processRepository.save(NotificationProcessedContent.of(content.getContentId()));

        log.info("[NotificationWorker] 처리 완료 - contentId: {}, 소요시간: {}ms", content.getContentId(), (System.nanoTime() - startTime) / 1_000_000);
    }

    private Map<Long, Set<String>> collectMatchedKeywords(Set<Long> blogSubscriberIds, Set<String> keywords) {

        Map<Long, Set<String>> userMatchedKeywords = new HashMap<>();

        for (String keyword : keywords) {
            String key = KEYWORD_KEY_PREFIX + keyword;

            // 키워드 키 존재 여부 확인
            if (!redisTemplate.hasKey(key)) {
                log.info("[NotificationConsumer] Redis 키 없음 - key: {}", key);
                continue;
            }

            ScanOptions options = ScanOptions
                    .scanOptions()
                    .count(CHUNK_SIZE)
                    .build();

            try (Cursor<String> cursor = redisTemplate.opsForSet().scan(key, options)) {

                while (cursor.hasNext()) {
                    Long userId = Long.parseLong(cursor.next());

                    if (!blogSubscriberIds.contains(userId)) {
                        continue;
                    }

                    userMatchedKeywords
                            .computeIfAbsent(userId, k -> new HashSet<>())
                            .add(keyword);
                }
            } catch (Exception e) {
                log.error("[NotificationConsumer] SSCAN 실패 - keyword: {}, error: {}", keyword, e.getMessage());
            }
        }

        return userMatchedKeywords;
    }

    private void saveNotificationsInChunks(Map<Long, Set<String>> userMatchedKeywords, ContentEventPayload content) {
        List<Long> userIds = new ArrayList<>(userMatchedKeywords.keySet());

        long totalInsertTime = 0;
        for (int i = 0; i < userIds.size(); i += CHUNK_SIZE) {
            List<Long> chunk = userIds.subList(i, Math.min(i + CHUNK_SIZE, userIds.size()));
            long insertStart = System.nanoTime();
            notificationJdbcRepository.bulkInsertNotifications(chunk, userMatchedKeywords, content);
            totalInsertTime += (System.nanoTime() - insertStart) / 1_000_000;
        }

        log.info("[NotificationConsumer] 배치 INSERT 완료 - {}명, {}ms", userIds.size(), totalInsertTime);
    }

    private Set<String> extractKeywords(String title, String summary) {
        if (title == null) {
            title = "";
        }
        if (summary == null) {
            summary = "";
        }
        String text = title + " " + summary;

        String[] tokens = text.replaceAll("[^a-zA-Z0-9가-힣\\s]", "").split("\\s+");

        return Arrays.stream(tokens)
                .filter(token -> token.length() >= 2)
                .collect(Collectors.toSet());
    }

    private ContentEventPayload deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, ContentEventPayload.class);
        } catch (JsonProcessingException e) {
            log.error("[NotificationConsumer] JSON 파싱 오류 - {}", e.getMessage());
            return null;
        }
    }
}
