package com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TossPaymentQueryResponse {
    private String orderId;
    private String paymentKey;
    private String status;
    private String approvedAt;
}
