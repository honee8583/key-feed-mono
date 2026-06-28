package com.keyfeed.keyfeedmonolithic.domain.auth.entity;

public enum PasswordResetAuditType {
    CODE_VERIFIED,   // 비밀번호 재설정 인증 코드 확인 성공
    PASSWORD_RESET   // 비밀번호 변경 완료
}
