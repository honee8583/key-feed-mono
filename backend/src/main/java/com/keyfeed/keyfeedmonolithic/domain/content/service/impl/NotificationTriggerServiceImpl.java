package com.keyfeed.keyfeedmonolithic.domain.content.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.content.service.NotificationTriggerService;
import com.keyfeed.keyfeedmonolithic.domain.crawl.dto.CrawledContentDto;
import com.keyfeed.keyfeedmonolithic.domain.keyword.repository.KeywordRepository;
import com.keyfeed.keyfeedmonolithic.domain.notification.dto.NotificationEventDto;
import com.keyfeed.keyfeedmonolithic.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTriggerServiceImpl implements NotificationTriggerService {

    private static final String NOTIFICATION_MESSAGE = "새로운 글이 등록되었습니다.";

    private final KeywordRepository keywordRepository;
    private final NotificationService notificationService;

    @Override
    public void matchAndSendNotification(CrawledContentDto content) {
        Set<String> keywords = extractKeywords(content.getTitle(), content.getSummary());
        if (keywords.isEmpty()) {
            return;
        }

        List<Long> userIds = keywordRepository.findUserIdsByNamesAndSourceId(keywords, content.getSourceId());

        if (userIds.isEmpty()) {
            return;
        }

        log.info("알림 대상 유저 {}명 발견. 콘텐츠: {}", userIds.size(), content.getTitle());

        for (Long userId : userIds) {
            NotificationEventDto event = NotificationEventDto.builder()
                    .userId(userId)
                    .title(content.getTitle())
                    .message(NOTIFICATION_MESSAGE)
                    .originalUrl(content.getOriginalUrl())
                    .build();
            notificationService.send(event);
        }
    }

    private Set<String> extractKeywords(String title, String summary) {
        if (title == null) {
            title = "";
        }
        if (summary == null) {
            summary = "";
        }
        String text = title + " " + summary;

        // 특수문자 제거 및 공백 기준 분리
        String[] tokens = text.replaceAll("[^a-zA-Z0-9가-힣\\s]", "").split("\\s+");

        return Arrays.stream(tokens)
                .filter(token -> token.length() >= 2) // 2글자 이상만
                .collect(Collectors.toSet());
    }
}
