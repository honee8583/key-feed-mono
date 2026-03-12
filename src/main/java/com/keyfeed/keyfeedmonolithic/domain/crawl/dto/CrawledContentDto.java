package com.keyfeed.keyfeedmonolithic.domain.crawl.dto;

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
}