package com.keyfeed.keyfeedmonolithic.domain.content.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.content.entity.Content;
import com.keyfeed.keyfeedmonolithic.domain.content.repository.ContentRepository;
import com.keyfeed.keyfeedmonolithic.domain.content.service.ContentService;
import com.keyfeed.keyfeedmonolithic.domain.crawl.dto.CrawledContentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final ContentRepository contentRepository;

    @Transactional
    public void saveContent(CrawledContentDto dto) {

        LocalDateTime now = LocalDateTime.now();
        Content content = Content.builder()
                .sourceId(dto.getSourceId())
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .originalUrl(dto.getOriginalUrl())
                .thumbnailUrl(dto.getThumbnailUrl())
                .publishedAt(dto.getPublishedAt())
                .createdAt(now)
                .updatedAt(now)
                .build();
        contentRepository.save(content);

        log.info("콘텐츠 저장 완료 (MySQL): {}", dto.getTitle());
    }
}
