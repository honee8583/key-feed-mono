package com.keyfeed.keyfeedmonolithic.domain.content.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keyfeed.keyfeedmonolithic.domain.content.entity.Content;
import com.keyfeed.keyfeedmonolithic.domain.content.repository.ContentRepository;
import com.keyfeed.keyfeedmonolithic.domain.content.service.ContentService;
import com.keyfeed.keyfeedmonolithic.domain.crawl.dto.CrawledContentDto;
import com.keyfeed.keyfeedmonolithic.infra.outbox.entity.Outbox;
import com.keyfeed.keyfeedmonolithic.infra.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final ContentRepository contentRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveContent(CrawledContentDto dto) {
        // TODO 중복 체크 hash

        Content content = Content.builder()
                .sourceId(dto.getSourceId())
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .originalUrl(dto.getOriginalUrl())
                .thumbnailUrl(dto.getThumbnailUrl())
                .publishedAt(dto.getPublishedAt())
                .build();
        contentRepository.save(content);

        String payload = toJson(Map.of(
                "contentId", content.getId(),
                "sourceId", content.getSourceId(),
                "title", content.getTitle(),
                "summary", content.getSummary(),
                "createdAt", content.getCreatedAt().toString()
        ));
        outboxRepository.save(Outbox.create(content.getId(), payload));

        log.info("[ContentService] 컨텐츠 + Outbox 저장 완료 - contentId: {}", content.getId());
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("[Outbox] JSON 직렬화 실패", e);
        }
    }
}
