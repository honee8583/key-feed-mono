package com.keyfeed.keyfeedmonolithic.domain.content.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.content.document.ContentDocument;
import com.keyfeed.keyfeedmonolithic.domain.content.repository.ContentDocumentRepository;
import com.keyfeed.keyfeedmonolithic.domain.content.service.ContentService;
import com.keyfeed.keyfeedmonolithic.domain.crawl.dto.CrawledContentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final ContentDocumentRepository contentDocumentRepository;

    @Transactional
    public void saveContent(CrawledContentDto dto) {

        String id = UUID.nameUUIDFromBytes(dto.getOriginalUrl().getBytes(StandardCharsets.UTF_8)).toString();

        ContentDocument contentDocument = ContentDocument.builder()
                .id(id)
                .sourceId(dto.getSourceId())
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .originalUrl(dto.getOriginalUrl())
                .thumbnailUrl(dto.getThumbnailUrl())
                .publishedAt(dto.getPublishedAt())
                .createdAt(LocalDateTime.now())
                .build();
        contentDocumentRepository.save(contentDocument);

        log.info("콘텐츠 저장 완료 (ES): {}", dto.getTitle());
    }
}
