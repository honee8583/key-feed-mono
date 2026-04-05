package com.keyfeed.keyfeedmonolithic.global.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SuccessMessage {
    WRITE_SUCCESS("저장에 성공하였습니다."),
    READ_SUCCESS("조회에 성공하였습니다."),
    UPDATE_SUCCESS("수정에 성공하였습니다."),
    DELETE_SUCCESS("삭제에 성공하였습니다."),

    // identity
    LOGIN_SUCCESS("로그인에 성공하였습니다."),
    CREATE_TOKENS("토큰발급에 성공했습니다."),
    EMAIL_SEND_SUCCESS("이메일 전송에 성공하였습니다."),
    EMAIL_VERIFIED("이메일 인증이 완료되었습니다."),
    URL_VALIDATION_SUCCESS("URL 검증 성공"),
    PASSWORD_CHANGE_SUCCESS("비밀번호가 성공적으로 변경되었습니다."),
    WITHDRAW_SUCCESS("회원 탈퇴가 완료되었습니다."),
    PASSWORD_RESET_EMAIL_SENT("비밀번호 재설정을 위한 인증 이메일이 발송되었습니다."),
    PASSWORD_RESET_SUCCESS("비밀번호가 성공적으로 재설정되었습니다."),

    // payment
    PAYMENT_METHOD_REGISTERED("결제 수단이 등록되었습니다."),
    PAYMENT_METHOD_LIST("결제 수단 목록 조회에 성공했습니다."),
    PAYMENT_METHOD_DELETED("결제 수단이 삭제되었습니다."),
    PAYMENT_METHOD_DEFAULT_CHANGED("기본 결제 수단이 변경되었습니다."),
    CUSTOMER_KEY_ISSUED("고객 키가 발급되었습니다."),
    SUBSCRIPTION_STARTED("구독이 시작되었습니다."),
    SUBSCRIPTION_STATUS_READ("구독 상태 조회에 성공했습니다."),
    SUBSCRIPTION_CANCELED("구독이 해지되었습니다."),
    SUBSCRIPTION_RESUMED("구독이 재개되었습니다."),
    SUBSCRIPTION_REFUNDED("구독이 취소되었습니다. 환불이 처리됩니다.");

    private final String message;
}
