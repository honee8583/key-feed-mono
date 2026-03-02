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
    PASSWORD_RESET_SUCCESS("비밀번호가 성공적으로 재설정되었습니다.");

    private final String message;
}
