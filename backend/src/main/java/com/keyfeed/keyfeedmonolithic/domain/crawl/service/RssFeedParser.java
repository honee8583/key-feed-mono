package com.keyfeed.keyfeedmonolithic.domain.crawl.service;

import com.keyfeed.keyfeedmonolithic.domain.crawl.dto.FeedItem;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RssFeedParser {

    private static final int SUMMARY_LENGTH = 200;

    public List<FeedItem> parse(String feedUrl) {
        List<FeedItem> items = new ArrayList<>();
        try {
            String xmlData;
            try (InputStream in = new URL(feedUrl).openStream()) {
                xmlData = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            // 0x00-0x08, 0x0B-0x0C, 0x0E-0x1F 범위의 문자를 제거
            // (\t, \n, \r 은 유지해야 하므로 제외)
            String cleanXml = xmlData.replaceAll("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f]", "");

            // 입력받은 feedUrl에 HTTP요청을 보내 XML데이터를 가져온다
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new StringReader(cleanXml)); // XML구조를 분석하여 자바 객체로 변환(SyncFeed 객체)

            // 각 게시글 처리
            // Rome 라이브러리는 원본이 RSS 2.0의 <item>이든 Atom 1.0의 <entry>이든 상관없이 SyndEntry라는 표준 객체로 통일해 준다
            log.info("파싱된 엔트리 수: {}", feed.getEntries().size());
            for (SyndEntry entry : feed.getEntries()) {
                String guid = (entry.getUri() != null) ? entry.getUri() : entry.getLink();
                String title = entry.getTitle();
                String link = entry.getLink();

                log.info("guid: {}, title: {}, link: {}", guid, title, link);

                // RSS의 날짜형식을 LocalDateTime으로 변환
                LocalDateTime pubDate = LocalDateTime.now();
                if (entry.getPublishedDate() != null) {
                    pubDate = LocalDateTime.ofInstant(entry.getPublishedDate().toInstant(), ZoneId.systemDefault());
                }

                String summaryHtml = (entry.getDescription() != null) ? entry.getDescription().getValue() : "";
                String cleanSummary = "";
                String thumbnailUrl = null;

                if (!summaryHtml.isEmpty()) {
                    Document doc = Jsoup.parse(summaryHtml);  // HTML문자열을 DOM구조로 변환하여 태그를 찾기 쉽게 만든다
                    Element img = doc.select("img").first();  // 이미지 태그 찾기
                    if (img != null) {
                        thumbnailUrl = img.attr("src");
                    }
                    cleanSummary = doc.text(); // HTML 태그 찾기
                    if (cleanSummary.length() > SUMMARY_LENGTH) {  // 200자 제한
                        cleanSummary = cleanSummary.substring(0, SUMMARY_LENGTH) + "...";  // 너무 길면 200자 요약으로 생성
                    }
                }

                // 본문에 이미지가 없을 경우 <enclosure>를 확인하여 이미지가 있는지 한번 더 찾는다
                if (thumbnailUrl == null && entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
                    var enclosure = entry.getEnclosures().get(0);
                    if (enclosure.getType() != null && enclosure.getType().startsWith("image")) {
                        thumbnailUrl = enclosure.getUrl();
                    }
                }

                items.add(new FeedItem(guid, title, link, cleanSummary, thumbnailUrl, pubDate));
            }
        } catch (Exception e) {
            log.error("RSS 피드 파싱 실패: URL={}, 에러={}", feedUrl, e.getMessage());
        }
        return items;
    }
}