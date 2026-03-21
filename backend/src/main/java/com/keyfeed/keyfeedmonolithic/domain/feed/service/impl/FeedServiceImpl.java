package com.keyfeed.keyfeedmonolithic.domain.feed.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.bookmark.service.BookmarkService;
import com.keyfeed.keyfeedmonolithic.domain.content.dto.ContentFeedResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.content.entity.Content;
import com.keyfeed.keyfeedmonolithic.domain.content.repository.ContentRepository;
import com.keyfeed.keyfeedmonolithic.domain.feed.service.FeedService;
import com.keyfeed.keyfeedmonolithic.domain.source.dto.SourceResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.source.service.SourceService;
import com.keyfeed.keyfeedmonolithic.global.error.exception.InternalServerProcessingException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
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
    public Map<Long, String> fetchUserSourceMapping(Long userId) {
        try {
            List<SourceResponseDto> userSources = sourceService.getSourcesByUser(userId);
            if (CollectionUtils.isEmpty(userSources)) {
                return Collections.emptyMap();
            }

            return userSources.stream()
                    .filter(source -> StringUtils.hasText(source.getUserDefinedName()))
                    .collect(Collectors.toMap(
                            SourceResponseDto::getSourceId,
                            SourceResponseDto::getUserDefinedName,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.error("소스 목록 조회 중 예상치 못한 오류 발생. userId: {}", userId, e);
            throw new InternalServerProcessingException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    @Override
    public CommonPageResponse<ContentFeedResponseDto> getPersonalizedFeedsFromMySQL(Long userId, Map<Long, String> sourceMapping, Long lastId, int size, String keyword) {
        // 사용자가 구독한 source 조회
        List<Long> sourceIds = sourceService.getSourcesByUser(userId)
                .stream()
                .map(SourceResponseDto::getSourceId)
                .collect(Collectors.toList());

        if (sourceIds.isEmpty()) {
            return CommonPageResponse.<ContentFeedResponseDto>builder()
                    .content(Collections.emptyList())
                    .hasNext(false)
                    .nextCursorId(null)
                    .build();
        }
        int safeSize = Math.min(size, 50);
        Pageable pageable = PageRequest.of(0, safeSize + 1);

        boolean hasKeyword = keyword != null && !keyword.isBlank();

        List<Content> contents;
        if (hasKeyword) {
            contents = lastId == null
                    ? contentRepository.searchFirstPage(sourceIds, keyword, pageable)
                    : contentRepository.searchNextPage(sourceIds, lastId, keyword, pageable);
        } else {
            contents = lastId == null
                    ? contentRepository.findFirstPage(sourceIds, pageable)
                    : contentRepository.findNextPage(sourceIds, lastId, pageable);
        }

        boolean hasNext = contents.size() > safeSize;
        if (hasNext) {
            contents = contents.subList(0, safeSize);
        }

        List<ContentFeedResponseDto> feeds = contents.stream()
                .map(content -> ContentFeedResponseDto.from(content, sourceMapping))
                .collect(Collectors.toList());

        if (userId != null && !feeds.isEmpty()) {
            try {
                List<String> contentIds = feeds.stream()
                        .map(ContentFeedResponseDto::getContentId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                if (!contentIds.isEmpty()) {
                    Map<String, Long> bookmarkMap = bookmarkService.getBookmarkMap(userId, contentIds);
                    if (bookmarkMap != null) {
                        feeds.forEach(feed -> {
                            if (feed.getContentId() != null) {
                                feed.setBookmarkId(bookmarkMap.get(feed.getContentId()));
                            }
                        });
                    }
                }
            } catch (Exception e) {
                log.error("북마크 정보 조회 실패. userId: {}", userId, e);
            }
        }

        Long nextCursorId = hasNext && !contents.isEmpty()
                ? contents.get(contents.size() - 1).getId()
                : null;

        return CommonPageResponse.<ContentFeedResponseDto>builder()
                .content(feeds)
                .hasNext(hasNext)
                .nextCursorId(nextCursorId)
                .build();
    }

}
