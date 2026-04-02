package com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossBillingChargeResponse {
    private String paymentKey;
    private String orderId;
    private String orderName;
    private String status;
    private long totalAmount;
    private String method;
    private String approvedAt;
    private String currency;
    private Card card;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Card {
        private String number;
        private String cardType;
        private Integer installmentPlanMonths;
        private boolean useCardPoint;
    }
}
