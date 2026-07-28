package com.keyfeed.keyfeedmonolithic.domain.search.service;

import com.keyfeed.keyfeedmonolithic.domain.search.dto.TrendingSearchResponseDto;

import java.util.List;

public interface TrendingSearchService {

    void increment(Long userId, String keyword);

    List<TrendingSearchResponseDto> getTrendingSearches();

}
