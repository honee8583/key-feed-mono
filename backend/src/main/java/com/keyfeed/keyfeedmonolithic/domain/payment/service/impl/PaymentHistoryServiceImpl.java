package com.keyfeed.keyfeedmonolithic.domain.payment.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.payment.dto.PaymentHistoryItemResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentHistory;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentHistoryStatus;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.InvalidPaymentStatusException;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.PaymentHistorySizeExceededException;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.PaymentHistoryRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.service.PaymentHistoryService;
import com.keyfeed.keyfeedmonolithic.global.response.CursorPage;
import com.keyfeed.keyfeedmonolithic.global.util.CursorPagination;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentHistoryServiceImpl implements PaymentHistoryService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final List<PaymentHistoryStatus> INTERNAL_STATUSES =
            List.of(PaymentHistoryStatus.READY, PaymentHistoryStatus.IN_PROGRESS);

    private final PaymentHistoryRepository paymentHistoryRepository;

    @Override
    public CursorPage<PaymentHistoryItemResponseDto> getPaymentHistory(Long userId, Long cursorId, int size, String statusStr) {
        // 1. size 최대값 검증
        if (size > MAX_PAGE_SIZE) {
            throw new PaymentHistorySizeExceededException();
        }

        // 2. status 파라미터 파싱 및 유효성 검증
        PaymentHistoryStatus status = null;
        if (statusStr != null) {
            try {
                status = PaymentHistoryStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidPaymentStatusException();
            }
            if (INTERNAL_STATUSES.contains(status)) {
                throw new InvalidPaymentStatusException();
            }
        }

        // 3. size + 1개 조회 (다음 페이지 존재 여부 판단)
        PageRequest pageable = PageRequest.of(0, size + 1);

        List<PaymentHistory> histories;
        if (status == null) {
            histories = paymentHistoryRepository.findByUserIdWithCursor(
                    userId, PaymentHistoryStatus.READY, cursorId, pageable);
        } else {
            histories = paymentHistoryRepository.findByUserIdAndStatusWithCursor(
                    userId, status, cursorId, pageable);
        }

        // 4. 커서 페이징 처리 및 DTO 변환
        CursorPage<PaymentHistory> cursorPage = CursorPagination.paginate(histories, size, PaymentHistory::getId);

        return new CursorPage<>(
                cursorPage.getContent().stream().map(PaymentHistoryItemResponseDto::from).toList(),
                cursorPage.getNextCursorId(),
                cursorPage.isHasNext()
        );
    }
}
