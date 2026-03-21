package com.keyfeed.keyfeedmonolithic.domain.source.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RobotsTxtValidator {

    private static final int TIMEOUT = 5000; // 5초
    private static final String USER_AGENT = "KeyFeedBot";

    /**
     * 특정 URL이 robots.txt에 의해 크롤링 가능한지 확인
     *
     * @param targetUrl 확인할 대상 URL
     * @return 크롤링 허용 여부
     */
    public boolean isAllowedToCrawl(String targetUrl) {
        try {
            URL url = new URL(targetUrl);
//            String robotsTxtUrl = url.getProtocol() + "://" + url.getHost() + "/robots.txt";
            String robotsTxtUrl = url.getProtocol() + "://" + url.getAuthority() + "/robots.txt";

            log.info("robots.txt 확인 시작: {}", robotsTxtUrl);

            List<String> disallowedPaths = parseRobotsTxt(robotsTxtUrl);
            String path = url.getPath().isEmpty() ? "/" : url.getPath();

            // Disallow 규칙과 비교
            for (String disallowedPath : disallowedPaths) {
                if (path.startsWith(disallowedPath)) {
                    log.warn("robots.txt에 의해 크롤링 금지된 경로: {} (규칙: {})", targetUrl, disallowedPath);
                    return false;
                }
            }

            log.info("robots.txt 검증 통과: {}", targetUrl);
            return true;

        } catch (Exception e) {
            // robots.txt가 없거나 접근 불가능한 경우 기본적으로 허용
            log.info("robots.txt 확인 실패 (기본 허용): {} - {}", targetUrl, e.getMessage());
            return true;
        }
    }

    /**
     * robots.txt 파일을 파싱하여 Disallow 규칙 추출
     *
     * @param robotsTxtUrl robots.txt URL
     * @return Disallow된 경로 목록
     */
    private List<String> parseRobotsTxt(String robotsTxtUrl) throws Exception {
        List<String> disallowedPaths = new ArrayList<>();

        HttpURLConnection conn = (HttpURLConnection) new URL(robotsTxtUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        conn.setRequestProperty("User-Agent", USER_AGENT);

        // robots.txt가 없으면 빈 리스트 반환
        if (conn.getResponseCode() != 200) {
            log.info("robots.txt 파일이 존재하지 않음: {}", robotsTxtUrl);
            return disallowedPaths;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isRelevantUserAgent = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // 주석 제거
                int commentIndex = line.indexOf('#');
                if (commentIndex != -1) {
                    line = line.substring(0, commentIndex).trim();
                }

                if (line.isEmpty()) {
                    continue;
                }

                String lowerLine = line.toLowerCase();

                // User-agent 확인
                if (lowerLine.startsWith("user-agent:")) {
                    String agent = line.substring("user-agent:".length()).trim().toLowerCase();
                    // * (모든 봇) 또는 우리 봇 이름과 일치하는 경우
                    isRelevantUserAgent = agent.equals("*") ||
                            agent.equals(USER_AGENT.toLowerCase());
                }
                // 현재 User-agent에 해당하는 Disallow 규칙 수집
                else if (isRelevantUserAgent && lowerLine.startsWith("disallow:")) {
                    String path = line.substring("disallow:".length()).trim();
                    if (!path.isEmpty()) {
                        disallowedPaths.add(path);
                        log.debug("Disallow 규칙 발견: {}", path);
                    }
                }
                // 다른 User-agent가 나오면 종료
                else if (lowerLine.startsWith("user-agent:")) {
                    isRelevantUserAgent = false;
                }
            }
        }

        log.info("robots.txt 파싱 완료. Disallow 규칙 개수: {}", disallowedPaths.size());
        return disallowedPaths;
    }
}