package com.keyfeed.keyfeedmonolithic.domain.search.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.search.repository.RecentSearchRedisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class RecentSearchServiceImplTest {

    @InjectMocks
    private RecentSearchServiceImpl recentSearchService;

    @Mock
    private RecentSearchRedisRepository recentSearchRedisRepository;

    @Test
    @DisplayName("검색어를 트리밍하여 저장한다")
    void 검색어_트리밍_후_저장() {
        // when
        recentSearchService.record(1L, "  spring  ");

        // then
        then(recentSearchRedisRepository).should(times(1)).add(1L, "spring");
    }

    @Test
    @DisplayName("빈 검색어는 저장하지 않는다")
    void 빈_검색어_저장_안함() {
        // when
        recentSearchService.record(1L, "   ");

        // then
        then(recentSearchRedisRepository).should(never()).add(anyLong(), anyString());
    }

    @Test
    @DisplayName("검색어가 null이면 저장하지 않는다")
    void null_검색어_저장_안함() {
        // when
        recentSearchService.record(1L, null);

        // then
        then(recentSearchRedisRepository).should(never()).add(anyLong(), anyString());
    }

    @Test
    @DisplayName("userId가 null이면 저장하지 않는다")
    void null_userId_저장_안함() {
        // when
        recentSearchService.record(null, "spring");

        // then
        then(recentSearchRedisRepository).should(never()).add(anyLong(), anyString());
    }

    @Test
    @DisplayName("최근 검색어 목록을 최신순으로 조회한다")
    void 최근_검색어_목록_조회() {
        // given
        given(recentSearchRedisRepository.findAll(1L)).willReturn(List.of("java", "spring"));

        // when
        List<String> result = recentSearchService.getRecentSearches(1L);

        // then
        assertThat(result).containsExactly("java", "spring");
    }

    @Test
    @DisplayName("특정 검색어를 삭제한다")
    void 특정_검색어_삭제() {
        // when
        recentSearchService.delete(1L, "spring");

        // then
        then(recentSearchRedisRepository).should(times(1)).remove(1L, "spring");
    }

    @Test
    @DisplayName("전체 검색어를 삭제한다")
    void 전체_검색어_삭제() {
        // when
        recentSearchService.deleteAll(1L);

        // then
        then(recentSearchRedisRepository).should(times(1)).removeAll(1L);
    }
}
