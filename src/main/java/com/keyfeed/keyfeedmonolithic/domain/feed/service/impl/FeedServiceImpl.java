package com.keyfeed.keyfeedmonolithic.domain.feed.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.bookmark.service.BookmarkService;
import com.keyfeed.keyfeedmonolithic.domain.content.document.ContentDocument;
import com.keyfeed.keyfeedmonolithic.domain.content.dto.ContentFeedResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.content.repository.ContentDocumentRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final SourceService sourceService;
    private final BookmarkService bookmarkService;
    private final ContentDocumentRepository contentDocumentRepository;

    private static final DateTimeFormatter ES_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                    .withZone(ZoneOffset.UTC);

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
    public CommonPageResponse<ContentFeedResponseDto> getPersonalizedFeeds(Long userId, Map<Long, String> sourceMapping, Long lastPublishedAt, int size) {
        if (sourceMapping == null || sourceMapping.isEmpty()) {
            return CommonPageResponse.<ContentFeedResponseDto>builder()
                    .content(Collections.emptyList())
                    .hasNext(false)
                    .nextCursorId(null)
                    .build();
        }

        List<Long> sourceIds = new ArrayList<>(sourceMapping.keySet());

        Pageable pageable = buildPageable(size);
        List<ContentDocument> documents = searchDocuments(sourceIds, lastPublishedAt, pageable);

        boolean hasNext = documents.size() > size;
        List<ContentDocument> resultList = trimResultList(documents, hasNext, size);

        List<ContentFeedResponseDto> feeds = resultList.stream()
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

        Long nextCursorId = getNextPublishedAt(hasNext, resultList);
        return CommonPageResponse.<ContentFeedResponseDto>builder()
                .content(feeds)
                .hasNext(hasNext)
                .nextCursorId(nextCursorId)
                .build();
    }

    @Override
    public List<ContentFeedResponseDto> getContentsByIds(List<String> contentIds) {
        Iterable<ContentDocument> contentDocuments = contentDocumentRepository.findAllById(contentIds);

        List<ContentFeedResponseDto> contents = new ArrayList<>();
        for (ContentDocument contentDocument : contentDocuments) {
            contents.add(ContentFeedResponseDto.from(contentDocument));
        }

        return contents;
    }

    private Pageable buildPageable(int size) {
        return PageRequest.of(0, size + 1, Sort.by(Sort.Direction.DESC, "publishedAt"));
    }

    private List<ContentDocument> searchDocuments(List<Long> sourceIds, Long lastId, Pageable pageable) {
        if (lastId == null) {
            return contentDocumentRepository.searchBySourceIdsFirstPage(sourceIds, pageable);
        }

        String lastPublishedAt = convertCursorMillisToEsDate(lastId);
        return contentDocumentRepository.searchBySourceIdsAndCursor(sourceIds, lastPublishedAt, pageable);
    }

    private String convertCursorMillisToEsDate(Long cursorMillis) {
        return ES_DATE_FORMATTER.format(Instant.ofEpochMilli(cursorMillis));
    }

    private List<ContentDocument> trimResultList(List<ContentDocument> contents, boolean hasNext, int size) {
        if (hasNext) {
            return contents.subList(0, size);
        }
        return contents;
    }

    private Long getNextPublishedAt(boolean hasNext, List<ContentDocument> contents) {
        if (hasNext && !contents.isEmpty()) {
            return contents.get(contents.size() - 1).getPublishedAt()
                    .atZone(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli();
        }
        return null;
    }

}
