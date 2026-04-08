package com.keyfeed.keyfeedmonolithic.domain.content.dto;

import com.keyfeed.keyfeedmonolithic.domain.content.entity.Content;
import com.keyfeed.keyfeedmonolithic.domain.source.dto.SourceResponseDto;
import lombok.*;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ContentFeedResponseDto {
    private String contentId; // content
    private String title; // content
    private String summary; // content
    private String sourceName; // content
    private String sourceLogoUrl; // source
    private String originalUrl; // content
    private String thumbnailUrl; // content
    private LocalDateTime publishedAt; // content

    private Long bookmarkId; // bookmark

    public static ContentFeedResponseDto from(Content content, SourceResponseDto source) {
        // 1. 소스 이름 결정 (사용자 정의 이름 -> 컨텐츠 기본 이름 순)
        String sourceName = Optional.ofNullable(source)
                .map(SourceResponseDto::getUserDefinedName)
                .filter(StringUtils::hasText)
                .orElse(content.getSourceName());

        // 2. 로고 URL 결정 (소스 정보가 있으면 가져오고 없으면 null)
        String sourceLogoUrl = Optional.ofNullable(source)
                .map(SourceResponseDto::getLogoUrl)
                .orElse(null);

        return ContentFeedResponseDto.builder()
                .contentId(String.valueOf(content.getId()))
                .title(content.getTitle())
                .summary(content.getSummary())
                .sourceName(sourceName)
                .sourceLogoUrl(sourceLogoUrl)
                .originalUrl(content.getOriginalUrl())
                .thumbnailUrl(content.getThumbnailUrl())
                .publishedAt(content.getPublishedAt())
                .build();
    }
}
