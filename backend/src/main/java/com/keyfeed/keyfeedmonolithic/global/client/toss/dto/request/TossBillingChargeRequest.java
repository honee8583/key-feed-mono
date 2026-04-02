package com.keyfeed.keyfeedmonolithic.global.client.toss.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TossBillingChargeRequest {
    private String customerKey;
    private long amount;
    private String orderId;
    private String orderName;
    private String customerEmail;
    private String customerName;
    private Integer taxFreeAmount;
}
