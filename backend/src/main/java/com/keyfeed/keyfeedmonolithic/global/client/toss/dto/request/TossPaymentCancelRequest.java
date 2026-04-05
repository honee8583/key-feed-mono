package com.keyfeed.keyfeedmonolithic.global.client.toss.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TossPaymentCancelRequest {
    private String cancelReason;
}
