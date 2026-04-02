package com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossBillingIssueResponse {
    private String billingKey;
    private String customerKey;
    private String method;
    private String cardCompany;
    private Card card;
    private String authenticatedAt;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Card {
        private String number;
        private String cardType;
        private String ownerType;
    }
}
