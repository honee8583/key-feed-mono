package com.keyfeed.keyfeedmonolithic.domain.source.validator;

import com.keyfeed.keyfeedmonolithic.global.message.SuccessMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.*;

import static com.keyfeed.keyfeedmonolithic.global.constant.HttpConstants.USER_AGENT;
import static com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage.*;

@Slf4j
@Component
public class UrlValidator {

    private static final int TIMEOUT = 10000; // 10초

    private static final String CONTENT_TYPE_XML = "xml";
    private static final String CONTENT_TYPE_RSS = "rss";
    private static final String CONTENT_TYPE_ATOM = "atom";
    private static final String CONTENT_TYPE_HTML = "html";

    public ValidationResult validate(String urlString) {
        try {
            // 1. URL 형식 검증
            URL url = new URL(urlString);

            // 2. 프로토콜 검증
            if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")) {
                return ValidationResult.invalid(URL_UNSUPPORTED_PROTOCOL.getMessage());
            }

            // 3. 접근 가능 여부 확인
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = conn.getResponseCode();
            String contentType = conn.getContentType();

            conn.disconnect();

            // 4. 응답 코드 확인
            if (responseCode >= 200 && responseCode < 300) {
                // 5. Content-Type 확인 (RSS/XML/HTML 여부)
                if (contentType != null && (isValidContentType(contentType))) {
                    log.info("URL 검증 성공: {} (Content-Type: {})", urlString, contentType);
                    return ValidationResult.valid();
                } else {
                    log.warn("Content-Type이 RSS/XML/HTML이 아님: {}", contentType);
                    return ValidationResult.warning(URL_INVALID_CONTENT_TYPE.getMessage());
                }
            } else if (responseCode == 403) {
                return ValidationResult.invalid(URL_ACCESS_FORBIDDEN.getMessage());
            } else if (responseCode == 404) {
                return ValidationResult.invalid(URL_NOT_FOUND.getMessage());
            } else if (responseCode >= 500) {
                return ValidationResult.invalid(URL_SERVER_ERROR.getMessage() + "(HTTP " + responseCode + ")");
            } else {
                return ValidationResult.invalid("HTTP 오류 (코드: " + responseCode + ")");
            }

        } catch (UnknownHostException e) {
            log.error("존재하지 않는 호스트: {}", urlString, e);
            return ValidationResult.invalid(URL_UNKNOWN_HOST.getMessage());
        } catch (SocketTimeoutException e) {
            log.error("연결 시간 초과: {}", urlString, e);
            return ValidationResult.invalid(URL_CONNECTION_TIMEOUT.getMessage());
        } catch (MalformedURLException e) {
            log.error("잘못된 URL 형식: {}", urlString, e);
            return ValidationResult.invalid(URL_MALFORMED_FORMAT.getMessage());
        } catch (Exception e) {
            log.error("URL 검증 실패: {}", urlString, e);
            return ValidationResult.invalid(URL_VALIDATION_FAILED.getMessage() + ": " + e.getMessage());
        }
    }

    private boolean isValidContentType(String contentType) {
        return contentType.contains(CONTENT_TYPE_XML) ||
                contentType.contains(CONTENT_TYPE_RSS) ||
                contentType.contains(CONTENT_TYPE_ATOM) ||
                contentType.contains(CONTENT_TYPE_HTML);
    }

    @Getter
    @AllArgsConstructor
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final ValidationLevel level;

        public static ValidationResult valid() {
            return new ValidationResult(true, SuccessMessage.URL_VALIDATION_SUCCESS.getMessage(), ValidationLevel.SUCCESS);
        }

        public static ValidationResult warning(String message) {
            return new ValidationResult(true, message, ValidationLevel.WARNING);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message, ValidationLevel.ERROR);
        }
    }

    public enum ValidationLevel {
        SUCCESS, WARNING, ERROR
    }
}