package com.keyfeed.keyfeedmonolithic.global.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorMessage {
    INTERNAL_SERVER_ERROR("서버 에러가 발생했습니다."),
    ENTITY_NOT_FOUND("데이터가 존재하지 않습니다. "),
    ENTITY_ALREADY_EXISTS("데이터가 이미 존재합니다. "),
    INVALID_INPUT_VALUE("입력값이 올바르지 않습니다."),

    // auth
    TOKEN_EXPIRED("만료된 토큰입니다."),
    INVALID_TOKEN("유효하지 않은 토큰입니다."),
    EMPTY_TOKEN("토큰이 존재하지 않습니다."),
    INVALID_PASSWORD("비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED("인증이 필요합니다."),
    FORBIDDEN("권한이 없습니다."),
    PASSWORD_MISMATCH("새 비밀번호가 일치하지 않습니다."),
    SAME_PASSWORD("현재 비밀번호와 동일한 비밀번호로 변경할 수 없습니다."),
    EMAIL_VERIFICATION_REQUIRED("이메일 인증이 완료되지 않았습니다."),

    // email
    EMAIL_ALREADY_EXISTS("이미 존재하는 이메일입니다."),
    EMAIL_VERIFICATION_FAILED("인증번호가 일치하지 않습니다."),
    EMAIL_VERIFICATION_EXPIRED("인증코드의 유효기간이 지났습니다."),
    EMAIL_VERIFICATION_LOCKED("일정 시간 동안 많은 시도로 인해 인증이 제한되었습니다."),
    EMAIL_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED("지정된 인증 횟수가 초과되었습니다."),
    EMAIL_SEND_FAILED("이메일 전송에 실패하였습니다."),
    EMAIL_ALREADY_VERIFIED("이미 인증된 이메일입니다."),

    // keyword
    KEYWORD_LIMIT_EXCEEDED("키워드 등록 한도를 넘었습니다."),

    // crawl
    INVALID_RSS_URL("해당 URL에 접근할 수 없거나 유효한 웹사이트가 아닙니다."),
    SOURCE_VALIDATION_FAILED("소스 검증에 실패했습니다."),
    ROBOTS_TXT_DISALLOWED("해당 사이트는 robots.txt 정책에 의해 크롤링이 금지되어 있습니다."),
    RSS_PARSING_FAILED("RSS 피드를 파싱할 수 없습니다. URL을 확인해주세요."),

    // bookmark
    BOOKMARK_FOLDER_LIMIT_EXCEEDED("북마크 폴더 생성 한도를 넘었습니다."),

    // payment
    INVALID_PAYMENT_METHOD("유효하지 않은 결제 수단입니다."),
    PAYMENT_FAILED("결제 처리에 실패했습니다."),
    TOSS_UNAUTHORIZED("결제 인증 키가 유효하지 않습니다."),
    INVALID_AUTH_KEY("만료되었거나 유효하지 않은 인증 키입니다."),
    DUPLICATE_PAYMENT_METHOD("이미 등록된 결제 수단입니다."),
    PAYMENT_METHOD_IN_USE("구독 중인 결제 수단은 삭제할 수 없습니다. 먼저 결제 수단을 변경해주세요."),
    PAYMENT_METHOD_NOT_FOUND("결제 수단을 찾을 수 없습니다."),
    PAYMENT_METHOD_ACCESS_DENIED("해당 결제 수단에 대한 권한이 없습니다."),
    ACTIVE_SUBSCRIPTION_ALREADY_EXISTS("이미 구독 중입니다."),
    SUBSCRIPTION_NOT_FOUND("활성화된 구독이 없습니다."),
    ALREADY_CANCELED("이미 해지 신청된 구독입니다."),
    PAUSED_SUBSCRIPTION_NOT_FOUND("일시 정지된 구독이 없습니다."),
    REFUND_PERIOD_EXPIRED("결제일로부터 1일이 지나 취소할 수 없습니다."),
    REFUND_FAILED("환불 처리에 실패했습니다."),
    PAYMENT_HISTORY_SIZE_EXCEEDED("최대 조회 가능 수는 50개입니다."),
    INVALID_PAYMENT_STATUS("유효하지 않은 상태값입니다."),

    // URL
    URL_UNKNOWN_HOST("존재하지 않는 주소입니다."),
    URL_CONNECTION_TIMEOUT("연결 시간이 초과되었습니다. 사이트가 응답하지 않습니다."),
    URL_MALFORMED_FORMAT("URL 형식이 올바르지 않습니다."),
    URL_UNSUPPORTED_PROTOCOL("지원하지 않는 프로토콜입니다. HTTP 또는 HTTPS만 가능합니다."),
    URL_ACCESS_FORBIDDEN("접근이 금지된 사이트입니다. (403 Forbidden)"),
    URL_NOT_FOUND("페이지를 찾을 수 없습니다. (404 Not Found)"),
    URL_SERVER_ERROR("서버 오류가 발생했습니다."),
    URL_VALIDATION_FAILED("URL 접근 중 오류가 발생했습니다."),
    URL_INVALID_CONTENT_TYPE("RSS 피드 형식이 아닐 수 있습니다.");

    private final String message;
}
