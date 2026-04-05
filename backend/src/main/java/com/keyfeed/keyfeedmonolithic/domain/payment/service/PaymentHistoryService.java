package com.keyfeed.keyfeedmonolithic.domain.payment.service;

import com.keyfeed.keyfeedmonolithic.domain.payment.dto.PaymentHistoryItemResponseDto;
import com.keyfeed.keyfeedmonolithic.global.response.CursorPage;

public interface PaymentHistoryService {

    CursorPage<PaymentHistoryItemResponseDto> getPaymentHistory(Long userId, Long cursorId, int size, String status);
}
