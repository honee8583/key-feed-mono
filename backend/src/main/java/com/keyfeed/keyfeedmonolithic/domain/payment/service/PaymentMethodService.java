package com.keyfeed.keyfeedmonolithic.domain.payment.service;

import com.keyfeed.keyfeedmonolithic.domain.payment.dto.PaymentMethodRegisterRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.payment.dto.PaymentMethodResponseDto;

import java.util.List;

public interface PaymentMethodService {

    String getOrCreateCustomerKey(Long userId);

    PaymentMethodResponseDto registerPaymentMethod(Long userId, PaymentMethodRegisterRequestDto dto);

    List<PaymentMethodResponseDto> getPaymentMethods(Long userId);

    void deletePaymentMethod(Long userId, Long methodId);

    void changeDefaultPaymentMethod(Long userId, Long methodId);
}
