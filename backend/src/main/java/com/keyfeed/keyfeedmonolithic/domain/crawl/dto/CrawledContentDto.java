package com.keyfeed.keyfeedmonolithic.domain.crawl.dto;

import com.keyfeed.keyfeedmonolithic.domain.content.entity.Content;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
public class CrawledContentDto {
    private Long sourceId;
    private String title;
    private String summary;
    private String originalUrl;
    private String thumbnailUrl;
    private LocalDateTime publishedAt;

    public static Content toEntity(CrawledContentDto dto) {
        return Content.builder()
                .sourceId(dto.getSourceId())
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .originalUrl(dto.getOriginalUrl())
                .thumbnailUrl(dto.getThumbnailUrl())
                .publishedAt(dto.getPublishedAt())
                .build();
    }
}