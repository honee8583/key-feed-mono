package com.keyfeed.keyfeedmonolithic.domain.crawl.service;

import com.keyfeed.keyfeedmonolithic.domain.content.service.ContentService;
import com.keyfeed.keyfeedmonolithic.domain.crawl.dto.CrawledContentDto;
import com.keyfeed.keyfeedmonolithic.domain.crawl.dto.FeedItem;
import com.keyfeed.keyfeedmonolithic.domain.crawl.dto.ParsedFeedResult;
import com.keyfeed.keyfeedmonolithic.domain.source.entity.Source;
import com.keyfeed.keyfeedmonolithic.domain.source.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlService {

    private final SourceRepository sourceRepository;
    private final RssFeedParser rssFeedParser;
    private final ContentService contentService;

    public void processSource(Source source) {
        StopWatch sw = new StopWatch("processSource");

        log.info("소스 크롤링 시작: {}", source.getUrl());

        // 1. RSS 파싱
        sw.start("RSS 파싱");
        ParsedFeedResult feedResult = rssFeedParser.parse(source.getUrl());
        List<FeedItem> items = feedResult.getItems();
        sw.stop();

        // 로고 URL이 아직 없으면 RSS 채널 이미지로 업데이트
        if (source.getLogoUrl() == null && feedResult.getLogoUrl() != null) {
            source.updateLogoUrl(feedResult.getLogoUrl());
        }

        if (items.isEmpty()) {
            updateSourceStatus(source, source.getLastItemHash()); // 시간만 갱신
            return;
        }

        // 2. 새 글 필터링
        String lastHash = source.getLastItemHash();
        List<FeedItem> newItems = new ArrayList<>();

        // RSS는 보통 최신순으로 정렬되어 있으므로 위에서부터 검사
        for (FeedItem item : items) {
            // 이전에 수집한 마지막 글(Hash)을 만나면 중단
            if (item.getGuid().equals(lastHash)) {
                break;
            }
            newItems.add(item);
        }

        if (newItems.isEmpty()) {
            log.info("새로운 글 없음: {}", source.getUrl());
            updateSourceStatus(source, lastHash); // 시간만 갱신
            return;
        }

        log.info("새 글 {}개 발견: {}", newItems.size(), source.getUrl());

        // 3. 컨텐츠 저장 & 알림 저장
        for (int i = newItems.size() - 1; i >= 0; i--) {
            sw.start("Content 저장");

            FeedItem item = newItems.get(i);

            CrawledContentDto contentDto = CrawledContentDto.builder()
                    .sourceId(source.getId())
                    .title(item.getTitle())
                    .summary(item.getSummary())
                    .originalUrl(item.getLink())
                    .thumbnailUrl(item.getThumbnailUrl())
                    .publishedAt(item.getPubDate())
                    .build();

            contentService.saveContent(contentDto);
            sw.stop();
            log.info("content 저장 완료");

//            notificationService.matchAndSendNotification(contentDto);  // TODO 분산 처리 방식으로 변경
        }

        // 4. Source 업데이트 최신화
        String newLatestHash = items.get(0).getGuid();  // 가장 최신글의 hash로 업데이트
        log.info("new Latest Hash: {}", newLatestHash);
        updateSourceStatus(source, newLatestHash);

        log.info("[sourceId : {}] 크롤링 완료", source.getUrl());

        System.out.println(sw.prettyPrint());
    }

    private void updateSourceStatus(Source source, String newHash) {
        source.updateLastCrawledAt(LocalDateTime.now());
        if (newHash != null) {
            source.updateLastItemHash(newHash);
        }
        sourceRepository.save(source);
    }
}
