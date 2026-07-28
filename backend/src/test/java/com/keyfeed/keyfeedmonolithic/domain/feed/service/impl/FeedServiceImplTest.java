package com.keyfeed.keyfeedmonolithic.domain.feed.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.bookmark.dto.BookmarkFeedInfoDto;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.service.BookmarkService;
import com.keyfeed.keyfeedmonolithic.domain.content.dto.ContentFeedResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.content.entity.Content;
import com.keyfeed.keyfeedmonolithic.domain.content.repository.ContentRepository;
import com.keyfeed.keyfeedmonolithic.domain.search.service.RecentSearchService;
import com.keyfeed.keyfeedmonolithic.domain.source.dto.SourceResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.source.service.SourceService;
import com.keyfeed.keyfeedmonolithic.global.response.CommonPageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceImplTest {

    @InjectMocks
    private FeedServiceImpl feedService;

    @Mock
    private SourceService sourceService;

    @Mock
    private BookmarkService bookmarkService;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private RecentSearchService recentSearchService;

    private SourceResponseDto buildSource(Long sourceId, String name, String logoUrl) {
        return SourceResponseDto.builder()
                .sourceId(sourceId)
                .userSourceId(sourceId)
                .name(name)
                .logoUrl(logoUrl)
                .receiveFeed(true)
                .build();
    }

    private Content buildContent(Long id, Long sourceId, String sourceName) {
        return Content.builder()
                .id(id)
                .sourceId(sourceId)
                .sourceName(sourceName)
                .title("title-" + id)
                .summary("summary-" + id)
                .originalUrl("http://example.com/" + id)
                .thumbnailUrl("http://img.com/" + id)
                .publishedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("구독 소스가 없으면 빈 피드를 반환한다")
    void 구독_소스가_없으면_빈_피드_반환() {
        // given
        given(sourceService.getSourcesByUser(1L)).willReturn(Collections.emptyList());

        // when
        CommonPageResponse<ContentFeedResponseDto> result = feedService.getPersonalizedFeeds(1L, null, 10, null);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getNextCursorId()).isNull();
        then(contentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("키워드 없이 첫 페이지 조회 시 findFirstPage를 호출한다")
    void 키워드_없이_첫_페이지_조회() {
        // given
        List<SourceResponseDto> sources = List.of(buildSource(1L, "MyBlog", "http://logo.com/1"));
        List<Content> contents = List.of(buildContent(10L, 1L, "Blog"));

        given(sourceService.getSourcesByUser(1L)).willReturn(sources);
        given(contentRepository.findFirstPage(anyList(), any(Pageable.class))).willReturn(contents);
        BookmarkFeedInfoDto info = BookmarkFeedInfoDto.builder()
                .bookmarkId(99L)
                .folderId(7L)
                .folderName("기술")
                .build();
        given(bookmarkService.getBookmarkInfoMap(anyLong(), anyList())).willReturn(Map.of("10", info));

        // when
        CommonPageResponse<ContentFeedResponseDto> result = feedService.getPersonalizedFeeds(1L, null, 10, null);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSourceName()).isEqualTo("MyBlog");
        assertThat(result.getContent().get(0).getSourceLogoUrl()).isEqualTo("http://logo.com/1");
        assertThat(result.getContent().get(0).getBookmarkId()).isEqualTo(99L);
        assertThat(result.getContent().get(0).getFolderId()).isEqualTo(7L);
        assertThat(result.getContent().get(0).getFolderName()).isEqualTo("기술");
        assertThat(result.isHasNext()).isFalse();
        then(sourceService).should(times(1)).getSourcesByUser(1L);
    }

    @Test
    @DisplayName("키워드 없이 다음 페이지 조회 시 findNextPage를 호출한다")
    void 키워드_없이_다음_페이지_조회() {
        // given
        List<SourceResponseDto> sources = List.of(buildSource(1L, null, null));
        List<Content> contents = List.of(buildContent(5L, 1L, "Blog"));

        given(sourceService.getSourcesByUser(1L)).willReturn(sources);
        given(contentRepository.findNextPage(anyList(), eq(10L), any(Pageable.class))).willReturn(contents);
        given(bookmarkService.getBookmarkInfoMap(anyLong(), anyList())).willReturn(Collections.emptyMap());

        // when
        CommonPageResponse<ContentFeedResponseDto> result = feedService.getPersonalizedFeeds(1L, 10L, 10, null);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSourceName()).isEqualTo("Blog");
        then(contentRepository).should().findNextPage(anyList(), eq(10L), any(Pageable.class));
    }

    @Test
    @DisplayName("키워드로 첫 페이지 검색 시 searchFirstPage를 호출한다")
    void 키워드로_첫_페이지_검색() {
        // given
        List<SourceResponseDto> sources = List.of(buildSource(1L, "Tech", "http://logo.com/1"));
        List<Content> contents = List.of(buildContent(7L, 1L, "TechBlog"));

        given(sourceService.getSourcesByUser(1L)).willReturn(sources);
        given(contentRepository.searchFirstPage(anyList(), eq("spring"), any(Pageable.class))).willReturn(contents);
        given(bookmarkService.getBookmarkInfoMap(anyLong(), anyList())).willReturn(Collections.emptyMap());

        // when
        CommonPageResponse<ContentFeedResponseDto> result = feedService.getPersonalizedFeeds(1L, null, 10, "spring");

        // then
        assertThat(result.getContent()).hasSize(1);
        then(contentRepository).should().searchFirstPage(anyList(), eq("spring"), any(Pageable.class));
    }

    @Test
    @DisplayName("키워드로 다음 페이지 검색 시 searchNextPage를 호출한다")
    void 키워드로_다음_페이지_검색() {
        // given
        List<SourceResponseDto> sources = List.of(buildSource(1L, null, null));
        List<Content> contents = List.of(buildContent(3L, 1L, "Blog"));

        given(sourceService.getSourcesByUser(1L)).willReturn(sources);
        given(contentRepository.searchNextPage(anyList(), eq(20L), eq("java"), any(Pageable.class))).willReturn(contents);
        given(bookmarkService.getBookmarkInfoMap(anyLong(), anyList())).willReturn(Collections.emptyMap());

        // when
        CommonPageResponse<ContentFeedResponseDto> result = feedService.getPersonalizedFeeds(1L, 20L, 10, "java");

        // then
        assertThat(result.getContent()).hasSize(1);
        then(contentRepository).should().searchNextPage(anyList(), eq(20L), eq("java"), any(Pageable.class));
    }

    @Test
    @DisplayName("hasNext가 true일 때 nextCursorId가 마지막 컨텐츠 ID로 설정된다")
    void hasNext_true_시_nextCursorId_설정() {
        // given
        List<SourceResponseDto> sources = List.of(buildSource(1L, null, null));
        // size=2 요청 → safeSize+1=3개 조회 → hasNext=true
        List<Content> contents = List.of(
                buildContent(10L, 1L, "Blog"),
                buildContent(9L, 1L, "Blog"),
                buildContent(8L, 1L, "Blog")
        );

        given(sourceService.getSourcesByUser(1L)).willReturn(sources);
        given(contentRepository.findFirstPage(anyList(), any(Pageable.class))).willReturn(contents);
        given(bookmarkService.getBookmarkInfoMap(anyLong(), anyList())).willReturn(Collections.emptyMap());

        // when
        CommonPageResponse<ContentFeedResponseDto> result = feedService.getPersonalizedFeeds(1L, null, 2, null);

        // then
        assertThat(result.isHasNext()).isTrue();
        assertThat(result.getNextCursorId()).isEqualTo(9L);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("size가 50을 초과하면 50으로 제한된다")
    void size가_50_초과_시_50으로_제한() {
        // given
        List<SourceResponseDto> sources = List.of(buildSource(1L, null, null));

        given(sourceService.getSourcesByUser(1L)).willReturn(sources);
        given(contentRepository.findFirstPage(anyList(), any(Pageable.class))).willReturn(Collections.emptyList());

        // when
        feedService.getPersonalizedFeeds(1L, null, 200, null);

        // then
        then(contentRepository).should().findFirstPage(anyList(), argThat(pageable -> pageable.getPageSize() == 51));
    }

    @Test
    @DisplayName("북마크 조회 실패 시 예외 없이 피드를 반환한다")
    void 북마크_조회_실패_시_피드_정상_반환() {
        // given
        List<SourceResponseDto> sources = List.of(buildSource(1L, null, null));
        List<Content> contents = List.of(buildContent(1L, 1L, "Blog"));

        given(sourceService.getSourcesByUser(1L)).willReturn(sources);
        given(contentRepository.findFirstPage(anyList(), any(Pageable.class))).willReturn(contents);
        given(bookmarkService.getBookmarkInfoMap(anyLong(), anyList())).willThrow(new RuntimeException("북마크 서비스 오류"));

        // when
        CommonPageResponse<ContentFeedResponseDto> result = feedService.getPersonalizedFeeds(1L, null, 10, null);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getBookmarkId()).isNull();
    }

    @Test
    @DisplayName("사용자정의명이 없는 소스는 원본 소스명을 사용한다")
    void 사용자정의명_없으면_원본_소스명_사용() {
        // given
        List<SourceResponseDto> sources = List.of(buildSource(1L, null, null));
        List<Content> contents = List.of(buildContent(1L, 1L, "OriginalSourceName"));

        given(sourceService.getSourcesByUser(1L)).willReturn(sources);
        given(contentRepository.findFirstPage(anyList(), any(Pageable.class))).willReturn(contents);
        given(bookmarkService.getBookmarkInfoMap(anyLong(), anyList())).willReturn(Collections.emptyMap());

        // when
        CommonPageResponse<ContentFeedResponseDto> result = feedService.getPersonalizedFeeds(1L, null, 10, null);

        // then
        assertThat(result.getContent().get(0).getSourceName()).isEqualTo("OriginalSourceName");
    }

    @Test
    @DisplayName("소스 조회를 단 한 번만 호출한다")
    void 소스_조회_단_한_번만_호출() {
        // given
        List<SourceResponseDto> sources = List.of(buildSource(1L, "Blog", "http://logo.com"));
        List<Content> contents = List.of(buildContent(1L, 1L, "Blog"));

        given(sourceService.getSourcesByUser(1L)).willReturn(sources);
        given(contentRepository.findFirstPage(anyList(), any(Pageable.class))).willReturn(contents);
        given(bookmarkService.getBookmarkInfoMap(anyLong(), anyList())).willReturn(Collections.emptyMap());

        // when
        feedService.getPersonalizedFeeds(1L, null, 10, null);

        // then
        then(sourceService).should(times(1)).getSourcesByUser(1L);
    }

    @Test
    @DisplayName("키워드 검색 첫 페이지 조회 시 최근 검색어를 저장한다")
    void 키워드_검색_첫_페이지_시_최근_검색어_저장() {
        // given
        List<SourceResponseDto> sources = List.of(buildSource(1L, null, null));
        List<Content> contents = List.of(buildContent(7L, 1L, "Blog"));

        given(sourceService.getSourcesByUser(1L)).willReturn(sources);
        given(contentRepository.searchFirstPage(anyList(), eq("spring"), any(Pageable.class))).willReturn(contents);
        given(bookmarkService.getBookmarkInfoMap(anyLong(), anyList())).willReturn(Collections.emptyMap());

        // when
        feedService.getPersonalizedFeeds(1L, null, 10, "spring");

        // then
        then(recentSearchService).should(times(1)).record(1L, "spring");
    }

    @Test
    @DisplayName("키워드 검색 다음 페이지 조회 시에는 최근 검색어를 저장하지 않는다")
    void 키워드_검색_다음_페이지_시_최근_검색어_저장_안함() {
        // given
        List<SourceResponseDto> sources = List.of(buildSource(1L, null, null));
        List<Content> contents = List.of(buildContent(3L, 1L, "Blog"));

        given(sourceService.getSourcesByUser(1L)).willReturn(sources);
        given(contentRepository.searchNextPage(anyList(), eq(20L), eq("java"), any(Pageable.class))).willReturn(contents);
        given(bookmarkService.getBookmarkInfoMap(anyLong(), anyList())).willReturn(Collections.emptyMap());

        // when
        feedService.getPersonalizedFeeds(1L, 20L, 10, "java");

        // then
        then(recentSearchService).should(never()).record(anyLong(), anyString());
    }

    @Test
    @DisplayName("키워드가 없으면 최근 검색어를 저장하지 않는다")
    void 키워드_없으면_최근_검색어_저장_안함() {
        // given
        List<SourceResponseDto> sources = List.of(buildSource(1L, null, null));
        List<Content> contents = List.of(buildContent(1L, 1L, "Blog"));

        given(sourceService.getSourcesByUser(1L)).willReturn(sources);
        given(contentRepository.findFirstPage(anyList(), any(Pageable.class))).willReturn(contents);
        given(bookmarkService.getBookmarkInfoMap(anyLong(), anyList())).willReturn(Collections.emptyMap());

        // when
        feedService.getPersonalizedFeeds(1L, null, 10, null);

        // then
        then(recentSearchService).should(never()).record(anyLong(), anyString());
    }

    @Test
    @DisplayName("최근 검색어 저장이 실패해도 피드는 정상 반환된다")
    void 최근_검색어_저장_실패해도_피드_정상_반환() {
        // given
        List<SourceResponseDto> sources = List.of(buildSource(1L, null, null));
        List<Content> contents = List.of(buildContent(7L, 1L, "Blog"));

        willThrow(new RuntimeException("Redis 연결 오류")).given(recentSearchService).record(1L, "spring");
        given(sourceService.getSourcesByUser(1L)).willReturn(sources);
        given(contentRepository.searchFirstPage(anyList(), eq("spring"), any(Pageable.class))).willReturn(contents);
        given(bookmarkService.getBookmarkInfoMap(anyLong(), anyList())).willReturn(Collections.emptyMap());

        // when
        CommonPageResponse<ContentFeedResponseDto> result = feedService.getPersonalizedFeeds(1L, null, 10, "spring");

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("오프셋 검색 첫 페이지 조회 시 최근 검색어를 저장한다")
    void 오프셋_검색_첫_페이지_시_최근_검색어_저장() {
        // given
        given(sourceService.getSourcesByUser(1L)).willReturn(Collections.emptyList());

        // when
        feedService.getPersonalizedFeedsWithOffset(1L, 0, 10, "spring");

        // then
        then(recentSearchService).should(times(1)).record(1L, "spring");
    }

    @Test
    @DisplayName("오프셋 검색 두 번째 페이지 조회 시에는 최근 검색어를 저장하지 않는다")
    void 오프셋_검색_두번째_페이지_시_최근_검색어_저장_안함() {
        // given
        given(sourceService.getSourcesByUser(1L)).willReturn(Collections.emptyList());

        // when
        feedService.getPersonalizedFeedsWithOffset(1L, 1, 10, "spring");

        // then
        then(recentSearchService).should(never()).record(anyLong(), anyString());
    }
}
