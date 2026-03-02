package com.keyfeed.keyfeedmonolithic.domain.keyword.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingKeywordResponseDto {
    private String name;
    private Long userCount;
}
