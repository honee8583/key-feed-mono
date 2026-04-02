package com.keyfeed.keyfeedmonolithic.global.client.toss;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.InvalidPaymentMethodException;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.PaymentFailedException;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.TossAuthException;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.request.TossBillingChargeRequest;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.request.TossBillingIssueRequest;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossBillingChargeResponse;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossBillingIssueResponse;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossErrorResponse;
import com.keyfeed.keyfeedmonolithic.global.error.exception.InternalApiRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Component
public class TossPaymentsClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String ERR_INVALID_CARD_EXPIRATION = "INVALID_CARD_EXPIRATION";
    private static final String ERR_CARD_PROCESSING_ERROR = "CARD_PROCESSING_ERROR";
    private static final String ERR_EXCEED_INSTALLMENT = "EXCEED_MAX_CARD_INSTALLMENT_PLAN";
    private static final String ERR_UNAUTHORIZED_KEY = "UNAUTHORIZED_KEY";

    private final RestTemplate restTemplate;
    private final TossPaymentsProperties properties;

    public TossPaymentsClient(@Qualifier("tossRestTemplate") RestTemplate restTemplate,
                              TossPaymentsProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public TossBillingIssueResponse issueBillingKey(TossBillingIssueRequest request) {
        URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/v1/billing/authorizations/issue")
                .build().toUri();
        return exchange(uri, HttpMethod.POST, request, TossBillingIssueResponse.class);
    }

    public TossBillingChargeResponse chargeBilling(String billingKey, TossBillingChargeRequest request) {
        URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/v1/billing/{billingKey}")
                .buildAndExpand(billingKey)
                .toUri();
        return exchange(uri, HttpMethod.POST, request, TossBillingChargeResponse.class);
    }

    public void deleteBillingKey(String billingKey) {
        URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/v1/billing/{billingKey}")
                .buildAndExpand(billingKey)
                .toUri();
        exchange(uri, HttpMethod.DELETE, null, Void.class);
    }

    private <T> T exchange(URI uri, HttpMethod method, Object body, Class<T> responseType) {
        HttpEntity<Object> entity = new HttpEntity<>(body);
        try {
            ResponseEntity<T> response = restTemplate.exchange(uri, method, entity, responseType);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            handleClientError(e);
            throw new InternalApiRequestException("Toss API 호출 실패: " + e.getMessage());
        } catch (ResourceAccessException e) {
            log.error("Toss API 네트워크 오류: {}", e.getMessage());
            throw new InternalApiRequestException("Toss API 네트워크 오류: " + e.getMessage());
        }
    }

    private void handleClientError(HttpClientErrorException e) {
        TossErrorResponse error = parseError(e.getResponseBodyAsString());
        if (error == null || error.getCode() == null) {
            return;
        }

        log.warn("Toss API 에러 - code: {}, message: {}", error.getCode(), error.getMessage());

        switch (error.getCode()) {
            case ERR_INVALID_CARD_EXPIRATION -> throw new InvalidPaymentMethodException();
            case ERR_CARD_PROCESSING_ERROR, ERR_EXCEED_INSTALLMENT -> throw new PaymentFailedException();
            case ERR_UNAUTHORIZED_KEY -> throw new TossAuthException();
            default -> log.warn("처리되지 않은 Toss 에러 코드: {}", error.getCode());
        }
    }

    private TossErrorResponse parseError(String body) {
        try {
            return MAPPER.readValue(body, TossErrorResponse.class);
        } catch (Exception e) {
            log.error("Toss 에러 응답 파싱 실패: {}", body);
            return null;
        }
    }
}
