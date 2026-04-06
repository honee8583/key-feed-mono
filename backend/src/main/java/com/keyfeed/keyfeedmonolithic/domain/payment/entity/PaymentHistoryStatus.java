package com.keyfeed.keyfeedmonolithic.domain.payment.entity;

public enum PaymentHistoryStatus {
    READY, // 결제 요청 전 선저장 상태
    IN_PROGRESS, // 결제인증/처리 중인 상태
    DONE, // 결제 최종 승인 완료
    CANCELED, // 결제 전액 취소
    PARTIAL_CANCELED, // 결제 부분 취소
    ABORTED, // 결제 인증 실패로 중단
    EXPIRED, // 결제 유효시간(30분) 초과로 만료
    FAILED // 결제 실패
}
