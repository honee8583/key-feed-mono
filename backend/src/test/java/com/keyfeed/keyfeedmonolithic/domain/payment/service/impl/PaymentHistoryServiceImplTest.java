package com.keyfeed.keyfeedmonolithic.domain.payment.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.payment.dto.PaymentHistoryItemResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.*;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.InvalidPaymentStatusException;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.PaymentHistorySizeExceededException;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.PaymentHistoryRepository;
import com.keyfeed.keyfeedmonolithic.global.response.CursorPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentHistoryServiceImplTest {

    @InjectMocks
    private PaymentHistoryServiceImpl paymentHistoryService;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    // ===== 정상 조회 =====

    @Test
    @DisplayName("결제 이력 조회 성공 - 첫 페이지 (cursorId null), READY 제외")
    void 결제이력_조회_성공_첫페이지() {
        // given
        Long userId = 1L;
        List<PaymentHistory> histories = List.of(
                makeHistory(5L, userId, PaymentHistoryStatus.DONE),
                makeHistory(4L, userId, PaymentHistoryStatus.FAILED)
        );

        given(paymentHistoryRepository.findByUserIdWithCursor(
                eq(userId), eq(PaymentHistoryStatus.READY), isNull(), any(Pageable.class)))
                .willReturn(histories);

        // when
        CursorPage<PaymentHistoryItemResponseDto> result =
                paymentHistoryService.getPaymentHistory(userId, null, 10, null);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getNextCursorId()).isEqualTo(4L);
    }

    @Test
    @DisplayName("결제 이력 조회 성공 - 다음 페이지 있을 때 hasNext=true, nextCursorId 반환")
    void 결제이력_조회_성공_다음페이지_있음() {
        // given
        Long userId = 1L;
        // size=2 요청 시 size+1=3개 조회
        List<PaymentHistory> histories = List.of(
                makeHistory(10L, userId, PaymentHistoryStatus.DONE),
                makeHistory(9L, userId, PaymentHistoryStatus.DONE),
                makeHistory(8L, userId, PaymentHistoryStatus.DONE)  // 초과 항목
        );

        given(paymentHistoryRepository.findByUserIdWithCursor(
                eq(userId), eq(PaymentHistoryStatus.READY), isNull(), any(Pageable.class)))
                .willReturn(histories);

        // when
        CursorPage<PaymentHistoryItemResponseDto> result =
                paymentHistoryService.getPaymentHistory(userId, null, 2, null);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.isHasNext()).isTrue();
        assertThat(result.getNextCursorId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("결제 이력 조회 성공 - cursorId 지정 시 해당 ID 이하 항목 반환")
    void 결제이력_조회_성공_커서ID_지정() {
        // given
        Long userId = 1L;
        Long cursorId = 8L;
        List<PaymentHistory> histories = List.of(
                makeHistory(7L, userId, PaymentHistoryStatus.DONE),
                makeHistory(6L, userId, PaymentHistoryStatus.CANCELED)
        );

        given(paymentHistoryRepository.findByUserIdWithCursor(
                eq(userId), eq(PaymentHistoryStatus.READY), eq(cursorId), any(Pageable.class)))
                .willReturn(histories);

        // when
        CursorPage<PaymentHistoryItemResponseDto> result =
                paymentHistoryService.getPaymentHistory(userId, cursorId, 10, null);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getPaymentId()).isEqualTo(7L);
        assertThat(result.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("결제 이력 조회 성공 - status=DONE 필터 적용")
    void 결제이력_조회_성공_DONE_필터() {
        // given
        Long userId = 1L;
        List<PaymentHistory> histories = List.of(
                makeHistory(5L, userId, PaymentHistoryStatus.DONE)
        );

        given(paymentHistoryRepository.findByUserIdAndStatusWithCursor(
                eq(userId), eq(PaymentHistoryStatus.DONE), isNull(), any(Pageable.class)))
                .willReturn(histories);

        // when
        CursorPage<PaymentHistoryItemResponseDto> result =
                paymentHistoryService.getPaymentHistory(userId, null, 10, "DONE");

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("DONE");
    }

    @Test
    @DisplayName("결제 이력 조회 성공 - FAILED 이력에 failReason 포함, approvedAt null")
    void 결제이력_조회_성공_FAILED_이력() {
        // given
        Long userId = 1L;
        List<PaymentHistory> histories = List.of(makeFailedHistory(3L, userId));

        given(paymentHistoryRepository.findByUserIdAndStatusWithCursor(
                eq(userId), eq(PaymentHistoryStatus.FAILED), isNull(), any(Pageable.class)))
                .willReturn(histories);

        // when
        CursorPage<PaymentHistoryItemResponseDto> result =
                paymentHistoryService.getPaymentHistory(userId, null, 10, "FAILED");

        // then
        assertThat(result.getContent().get(0).getFailReason()).isNotNull();
        assertThat(result.getContent().get(0).getApprovedAt()).isNull();
    }

    @Test
    @DisplayName("결제 이력 조회 성공 - 이력 없을 때 빈 배열 반환 (404 아님)")
    void 결제이력_조회_성공_빈목록() {
        // given
        Long userId = 1L;
        given(paymentHistoryRepository.findByUserIdWithCursor(
                eq(userId), eq(PaymentHistoryStatus.READY), isNull(), any(Pageable.class)))
                .willReturn(List.of());

        // when
        CursorPage<PaymentHistoryItemResponseDto> result =
                paymentHistoryService.getPaymentHistory(userId, null, 10, null);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getNextCursorId()).isNull();
    }

    @Test
    @DisplayName("결제 이력 조회 성공 - paymentMethod null이어도 정상 응답")
    void 결제이력_조회_성공_결제수단_null() {
        // given
        Long userId = 1L;
        given(paymentHistoryRepository.findByUserIdWithCursor(
                eq(userId), eq(PaymentHistoryStatus.READY), isNull(), any(Pageable.class)))
                .willReturn(List.of(makeHistoryWithoutMethod(1L, userId)));

        // when
        CursorPage<PaymentHistoryItemResponseDto> result =
                paymentHistoryService.getPaymentHistory(userId, null, 10, null);

        // then
        assertThat(result.getContent().get(0).getPaymentMethod()).isNull();
    }

    @Test
    @DisplayName("결제 이력 조회 성공 - status 소문자도 정상 처리")
    void 결제이력_조회_성공_소문자_status() {
        // given
        Long userId = 1L;
        given(paymentHistoryRepository.findByUserIdAndStatusWithCursor(
                eq(userId), eq(PaymentHistoryStatus.DONE), isNull(), any(Pageable.class)))
                .willReturn(List.of());

        // when & then
        assertThatCode(() -> paymentHistoryService.getPaymentHistory(userId, null, 10, "done"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("결제 이력 조회 성공 - size가 정확히 50이면 정상 처리")
    void 결제이력_조회_성공_size_최대값() {
        // given
        Long userId = 1L;
        given(paymentHistoryRepository.findByUserIdWithCursor(
                eq(userId), eq(PaymentHistoryStatus.READY), isNull(), any(Pageable.class)))
                .willReturn(List.of());

        // when & then
        assertThatCode(() -> paymentHistoryService.getPaymentHistory(userId, null, 50, null))
                .doesNotThrowAnyException();
    }

    // ===== 예외 처리 =====

    @Test
    @DisplayName("결제 이력 조회 실패 - size 50 초과 시 400 예외")
    void 결제이력_조회_실패_size초과() {
        // given
        Long userId = 1L;

        // when & then
        assertThatThrownBy(() -> paymentHistoryService.getPaymentHistory(userId, null, 51, null))
                .isInstanceOf(PaymentHistorySizeExceededException.class);
    }

    @Test
    @DisplayName("결제 이력 조회 실패 - 유효하지 않은 status 값이면 400 예외")
    void 결제이력_조회_실패_유효하지않은_status() {
        // given
        Long userId = 1L;

        // when & then
        assertThatThrownBy(() -> paymentHistoryService.getPaymentHistory(userId, null, 10, "INVALID_VALUE"))
                .isInstanceOf(InvalidPaymentStatusException.class);
    }

    @Test
    @DisplayName("결제 이력 조회 실패 - status=READY 요청 시 400 예외")
    void 결제이력_조회_실패_READY_status_요청() {
        // when & then
        assertThatThrownBy(() -> paymentHistoryService.getPaymentHistory(1L, null, 10, "READY"))
                .isInstanceOf(InvalidPaymentStatusException.class);
    }

    @Test
    @DisplayName("결제 이력 조회 실패 - status=IN_PROGRESS 요청 시 400 예외")
    void 결제이력_조회_실패_IN_PROGRESS_status_요청() {
        // when & then
        assertThatThrownBy(() -> paymentHistoryService.getPaymentHistory(1L, null, 10, "IN_PROGRESS"))
                .isInstanceOf(InvalidPaymentStatusException.class);
    }

    // ===== helpers =====

    private User makeUser(Long id) {
        return User.builder().id(id).email("test@test.com").username("테스터").build();
    }

    private PaymentMethod makePaymentMethod(User user) {
        return PaymentMethod.builder()
                .id(1L).user(user).billingKey("billing-key")
                .methodType(PaymentMethodType.CARD)
                .providerName("현대").displayNumber("1234****5678")
                .isDefault(true).isActive(true).build();
    }

    private PaymentHistory makeHistory(Long id, Long userId, PaymentHistoryStatus status) {
        User user = makeUser(userId);
        return PaymentHistory.builder()
                .id(id).user(user).paymentMethod(makePaymentMethod(user))
                .orderId("order-" + id).orderName("프리미엄 구독 1개월")
                .amount(9900).methodType(PaymentMethodType.CARD).status(status)
                .approvedAt(status == PaymentHistoryStatus.DONE ? LocalDateTime.now() : null)
                .createdAt(LocalDateTime.now()).build();
    }

    private PaymentHistory makeFailedHistory(Long id, Long userId) {
        User user = makeUser(userId);
        return PaymentHistory.builder()
                .id(id).user(user).paymentMethod(makePaymentMethod(user))
                .orderId("order-" + id).orderName("프리미엄 구독 1개월")
                .amount(9900).methodType(PaymentMethodType.CARD)
                .status(PaymentHistoryStatus.FAILED)
                .failReason("EXCEED_MAX_DAILY_PAYMENT_COUNT")
                .createdAt(LocalDateTime.now()).build();
    }

    private PaymentHistory makeHistoryWithoutMethod(Long id, Long userId) {
        User user = makeUser(userId);
        return PaymentHistory.builder()
                .id(id).user(user).paymentMethod(null)
                .orderId("order-" + id).orderName("프리미엄 구독 1개월")
                .amount(9900).status(PaymentHistoryStatus.DONE)
                .approvedAt(LocalDateTime.now()).createdAt(LocalDateTime.now()).build();
    }
}
