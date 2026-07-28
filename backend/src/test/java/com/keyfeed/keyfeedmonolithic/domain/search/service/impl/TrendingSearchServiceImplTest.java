package com.keyfeed.keyfeedmonolithic.domain.search.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.search.dto.TrendingSearchResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.search.repository.TrendingSearchRedisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class TrendingSearchServiceImplTest {

    @InjectMocks
    private TrendingSearchServiceImpl trendingSearchService;

    @Mock
    private TrendingSearchRedisRepository trendingSearchRedisRepository;

    @Test
    @DisplayName("검색어를 트리밍하여 집계한다")
    void 검색어_트리밍_후_집계() {
        // when
        trendingSearchService.increment(1L, "  spring  ");

        // then
        then(trendingSearchRedisRepository).should(times(1)).increment(1L, "spring");
    }

    @Test
    @DisplayName("빈 검색어는 집계하지 않는다")
    void 빈_검색어_집계_안함() {
        // when
        trendingSearchService.increment(1L, "   ");

        // then
        then(trendingSearchRedisRepository).should(never()).increment(anyLong(), anyString());
    }

    @Test
    @DisplayName("userId가 null이면 집계하지 않는다")
    void null_userId_집계_안함() {
        // when
        trendingSearchService.increment(null, "spring");

        // then
        then(trendingSearchRedisRepository).should(never()).increment(anyLong(), anyString());
    }

    @Test
    @DisplayName("검색 횟수 내림차순 순서대로 1부터 순위를 부여한다")
    void 순위_부여_및_매핑() {
        // given
        given(trendingSearchRedisRepository.findTopWithScores()).willReturn(List.of(
                new DefaultTypedTuple<>("spring", 42.0),
                new DefaultTypedTuple<>("java", 17.0),
                new DefaultTypedTuple<>("redis", 3.0)
        ));

        // when
        List<TrendingSearchResponseDto> result = trendingSearchService.getTrendingSearches();

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getRank()).isEqualTo(1);
        assertThat(result.get(0).getKeyword()).isEqualTo("spring");
        assertThat(result.get(0).getCount()).isEqualTo(42L);
        assertThat(result.get(1).getRank()).isEqualTo(2);
        assertThat(result.get(1).getKeyword()).isEqualTo("java");
        assertThat(result.get(2).getRank()).isEqualTo(3);
        assertThat(result.get(2).getKeyword()).isEqualTo("redis");
    }

    @Test
    @DisplayName("집계된 검색어가 없으면 빈 목록을 반환한다")
    void 집계_없으면_빈_목록_반환() {
        // given
        given(trendingSearchRedisRepository.findTopWithScores()).willReturn(List.of());

        // when
        List<TrendingSearchResponseDto> result = trendingSearchService.getTrendingSearches();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("score가 null인 항목은 count 0으로 매핑한다")
    void score_null이면_count_0() {
        // given
        given(trendingSearchRedisRepository.findTopWithScores()).willReturn(List.of(
                new DefaultTypedTuple<>("spring", null)
        ));

        // when
        List<TrendingSearchResponseDto> result = trendingSearchService.getTrendingSearches();

        // then
        assertThat(result.get(0).getCount()).isZero();
    }
}
