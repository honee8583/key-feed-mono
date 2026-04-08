package com.keyfeed.keyfeedmonolithic.domain.feed.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.bookmark.service.BookmarkService;
import com.keyfeed.keyfeedmonolithic.domain.content.dto.ContentFeedResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.content.entity.Content;
import com.keyfeed.keyfeedmonolithic.domain.content.repository.ContentRepository;
import com.keyfeed.keyfeedmonolithic.domain.feed.service.FeedService;
import com.keyfeed.keyfeedmonolithic.domain.source.dto.SourceResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.source.service.SourceService;
import com.keyfeed.keyfeedmonolithic.global.response.CommonPageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final SourceService sourceService;
    private final BookmarkService bookmarkService;
    private final ContentRepository contentRepository;

    @Override
    public CommonPageResponse<ContentFeedResponseDto> getPersonalizedFeeds(Long userId, Long lastId, int size, String keyword) {
        List<SourceResponseDto> userSources = sourceService.getSourcesByUser(userId);
        if (CollectionUtils.isEmpty(userSources)) {
            return CommonPageResponse.empty();
        }

        Map<Long, SourceResponseDto> sourceMap = userSources.stream()
                .collect(Collectors.toMap(
                        SourceResponseDto::getSourceId,
                        source -> source,
                        (existing, replacement) -> existing
                ));

        List<Long> sourceIds = new ArrayList<>(sourceMap.keySet());

        int safeSize = Math.min(size, 50);
        List<Content> contents = fetchContents(sourceIds, lastId, keyword, safeSize);

        boolean hasNext = contents.size() > safeSize;
        List<Content> pagedContents = hasNext ? contents.subList(0, safeSize) : contents;

        List<ContentFeedResponseDto> feeds = pagedContents.stream()
                .map(content -> ContentFeedResponseDto.from(content, sourceMap.get(content.getSourceId())))
                .collect(Collectors.toList());

        attachBookmarkStatus(userId, feeds);

        Long nextCursorId = resolveNextCursorId(hasNext, pagedContents);

        return CommonPageResponse.<ContentFeedResponseDto>builder()
                .content(feeds)
                .hasNext(hasNext)
                .nextCursorId(nextCursorId)
                .build();
    }

    private List<Content> fetchContents(List<Long> sourceIds, Long lastId, String keyword, int size) {
        Pageable pageable = PageRequest.of(0, size + 1);

        if (StringUtils.hasText(keyword)) {
            if (lastId == null) {
                return contentRepository.searchFirstPage(sourceIds, keyword, pageable);
            }
            return contentRepository.searchNextPage(sourceIds, lastId, keyword, pageable);
        }

        if (lastId == null) {
            return contentRepository.findFirstPage(sourceIds, pageable);
        }
        return contentRepository.findNextPage(sourceIds, lastId, pageable);
    }

    private void attachBookmarkStatus(Long userId, List<ContentFeedResponseDto> feeds) {
        if (userId == null || feeds.isEmpty()) {
            return;
        }

        List<String> contentIds = feeds.stream()
                .map(ContentFeedResponseDto::getContentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, Long> bookmarkMap = bookmarkService.getBookmarkMap(userId, contentIds);
        if (bookmarkMap != null) {
            feeds.forEach(feed -> feed.setBookmarkId(bookmarkMap.get(feed.getContentId())));
        }
    }

    private Long resolveNextCursorId(boolean hasNext, List<Content> contents) {
        if (hasNext && !contents.isEmpty()) {
            return contents.get(contents.size() - 1).getId();
        }
        return null;
    }
}
