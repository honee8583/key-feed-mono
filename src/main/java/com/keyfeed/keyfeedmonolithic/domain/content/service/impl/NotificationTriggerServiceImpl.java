//package com.keyfeed.keyfeedmonolithic.domain.content.service.impl;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.keyfeed.keyfeedmonolithic.domain.content.service.NotificationTriggerService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class NotificationTriggerServiceImpl implements NotificationTriggerService {
//
////    private final UserInternalApiClient userInternalApiClient;
////    private final ObjectMapper objectMapper;
////    private final KafkaTemplate<String, String> kafkaTemplate;
//
//    private final Snowflake snowflake;
//
//    @Value("${app.kafka.topic.notification}")
//    private String notificationTopic;
//
//    /**
//     * 콘텐츠의 키워드를 구독 중인 유저를 찾고 알림 이벤트를 발행
//     */
//    @Override
//    public void matchAndSendNotification(CrawledContentDto content) {
//        // 키워드 추출
//        Set<String> keywords = extractKeywords(content.getTitle(), content.getSummary());
//        if (keywords.isEmpty()) {
//            return;
//        }
//
//        List<Long> userIds = userInternalApiClient.findUserIdsByKeywordsAndSource(keywords, content.getSourceId());
//
//        if (userIds.isEmpty()) {
//            return;
//        }
//
//        log.info("알림 대상 유저 {}명 발견. 콘텐츠: {}", userIds.size(), content.getTitle());
//
//        // 각 유저에게 알림 메시지 발행
//        for (Long userId : userIds) {
//            sendNotificationKafkaMessage(userId, content);
//        }
//    }
//
//    private void sendNotificationKafkaMessage(Long userId, CrawledContentDto content) {
//        try {
//            NotificationEventDto event = NotificationEventDto.builder()
//                    .userId(userId)
//                    .notificationId(snowflake.nextId())
//                    .title(content.getTitle())
//                    .message(KafkaMessage.NOTIFICATION_MESSAGE.getMessage())
//                    .originalUrl(content.getOriginalUrl())
//                    .build();
//            String message = objectMapper.writeValueAsString(event);
//            kafkaTemplate.send(notificationTopic, message);
//        } catch (JsonProcessingException e) {
//            log.error("Kafka 전송을 위한 JSON 변환 실패. ContentTitle: {}", content.getTitle(), e);
//            throw new KafkaMessageProcessingException();
//        }
//    }
//
//    private Set<String> extractKeywords(String title, String summary) {
//        if (title == null) {
//            title = "";
//        }
//        if (summary == null) {
//            summary = "";
//        }
//        String text = title + " " + summary;
//
//        // 특수문자 제거 및 공백 기준 분리
//        String[] tokens = text.replaceAll("[^a-zA-Z0-9가-힣\\s]", "").split("\\s+");
//
//        return Arrays.stream(tokens)
//                .filter(token -> token.length() >= 2) // 2글자 이상만
//                .collect(Collectors.toSet());
//    }
//}