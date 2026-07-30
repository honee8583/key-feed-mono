package com.keyfeed.keyfeedmonolithic.domain.search.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.search.dto.TrendingSearchResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.search.repository.TrendingSearchRedisRepository;
import com.keyfeed.keyfeedmonolithic.domain.search.service.TrendingSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrendingSearchServiceImpl implements TrendingSearchService {

    private final TrendingSearchRedisRepository trendingSearchRedisRepository;

    @Override
    public void increment(Long userId, String keyword) {
        if (userId == null || !StringUtils.hasText(keyword)) {
            return;
        }
        trendingSearchRedisRepository.increment(userId, keyword.trim());
    }

    @Override
    public List<TrendingSearchResponseDto> getTrendingSearches() {
        List<TypedTuple<String>> tuples = trendingSearchRedisRepository.findTopWithScores();

        List<TrendingSearchResponseDto> result = new ArrayList<>();
        for (int i = 0; i < tuples.size(); i++) {
            TypedTuple<String> tuple = tuples.get(i);
            result.add(TrendingSearchResponseDto.builder()
                    .rank(i + 1)
                    .keyword(tuple.getValue())
                    .count(tuple.getScore() == null ? 0L : tuple.getScore().longValue())
                    .build());
        }
        return result;
    }
}
