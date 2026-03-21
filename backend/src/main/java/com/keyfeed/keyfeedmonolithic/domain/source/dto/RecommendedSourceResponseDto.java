package com.keyfeed.keyfeedmonolithic.domain.source.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedSourceResponseDto {
    private Long sourceId;
    private String url;
    private Long subscriberCount;
}
