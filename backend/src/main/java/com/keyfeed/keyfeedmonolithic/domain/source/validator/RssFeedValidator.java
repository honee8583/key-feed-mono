package com.keyfeed.keyfeedmonolithic.domain.source.validator;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import static com.keyfeed.keyfeedmonolithic.global.constant.HttpConstants.USER_AGENT;

@Slf4j
@Component
public class RssFeedValidator {

    private static final int TIMEOUT = 10000; // 10초

    /**
     * RSS 피드가 실제로 파싱 가능한지 검증
     *
     * @param feedUrl RSS 피드 URL
     * @return 파싱 가능 여부
     */
    public boolean canParseFeed(String feedUrl) {
        try {
            log.info("RSS 피드 파싱 테스트 시작: {}", feedUrl);

            // XML 데이터 가져오기
            URLConnection con = new URL(feedUrl).openConnection();
            con.setConnectTimeout(TIMEOUT); // 10초
            con.setReadTimeout(TIMEOUT);    // 10초
            con.setRequestProperty("User-Agent", USER_AGENT);

            String xmlData;
            try (InputStream in = con.getInputStream()) {
                xmlData = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            // 제어 문자 제거 (0x00-0x08, 0x0B-0x0C, 0x0E-0x1F)
            String cleanXml = xmlData.replaceAll("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f]", "");

            // RSS 파싱 시도
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new StringReader(cleanXml));

            // 최소한 제목이나 항목이 있는지 확인
            if (feed.getTitle() == null && (feed.getEntries() == null || feed.getEntries().isEmpty())) {
                log.warn("RSS 피드에 유효한 콘텐츠가 없음: {}", feedUrl);
                return false;
            }

            int itemCount = feed.getEntries() != null ? feed.getEntries().size() : 0;
            log.info("RSS 피드 파싱 성공: {} (제목: {}, 항목 수: {})", feedUrl, feed.getTitle(), itemCount);

            return true;

        } catch (Exception e) {
            log.error("RSS 피드 파싱 실패: {} - {}", feedUrl, e.getMessage());
            return false;
        }
    }

}