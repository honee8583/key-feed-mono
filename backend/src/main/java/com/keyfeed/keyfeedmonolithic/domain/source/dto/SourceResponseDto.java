package com.keyfeed.keyfeedmonolithic.domain.source.dto;

import com.keyfeed.keyfeedmonolithic.domain.source.entity.UserSource;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SourceResponseDto {
    private Long sourceId;
    private Long userSourceId;
    private String userDefinedName;
    private String url;
    private LocalDateTime lastCrawledAt;
    private Boolean receiveFeed;

    public static SourceResponseDto from(UserSource userSource) {
        return SourceResponseDto.builder()
                .sourceId(userSource.getSource().getId())
                .userSourceId(userSource.getId())
                .userDefinedName(userSource.getUserDefinedName())
                .url(userSource.getSource().getUrl())
                .lastCrawledAt(userSource.getSource().getLastCrawledAt())
                .receiveFeed(userSource.getReceiveFeed())
                .build();
    }
}
