package com.keyfeed.keyfeedmonolithic.domain.content.dto;

import com.keyfeed.keyfeedmonolithic.domain.content.entity.Content;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ContentFeedResponseDto {
    private String contentId;
    private String title;
    private String summary;
    private String sourceName;
    private String originalUrl;
    private String thumbnailUrl;
    private LocalDateTime publishedAt;

    private Long bookmarkId;

    public static ContentFeedResponseDto from(Content content, Map<Long, String> sourceMapping) {
        String sourceName = sourceMapping.getOrDefault(content.getSourceId(), content.getSourceName());
        return ContentFeedResponseDto.builder()
                .contentId(String.valueOf(content.getId()))
                .title(content.getTitle())
                .summary(content.getSummary())
                .sourceName(sourceName)
                .originalUrl(content.getOriginalUrl())
                .thumbnailUrl(content.getThumbnailUrl())
                .publishedAt(content.getPublishedAt())
                .build();
    }
}
