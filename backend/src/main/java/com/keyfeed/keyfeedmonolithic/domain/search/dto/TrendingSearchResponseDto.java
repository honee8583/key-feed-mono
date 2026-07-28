package com.keyfeed.keyfeedmonolithic.domain.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TrendingSearchResponseDto {

    private final int rank;
    private final String keyword;
    private final long count;

}
