package com.keyfeed.keyfeedmonolithic.domain.payment.dto;

import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentHistory;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossBillingChargeResponse;

public record ChargeResult(PaymentHistory history, TossBillingChargeResponse chargeResponse) {
}
