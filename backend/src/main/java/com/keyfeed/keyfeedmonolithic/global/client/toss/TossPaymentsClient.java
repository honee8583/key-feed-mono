package com.keyfeed.keyfeedmonolithic.global.client.toss;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.InvalidPaymentMethodException;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.PaymentFailedException;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.TossAuthException;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.request.TossBillingChargeRequest;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.request.TossBillingIssueRequest;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.request.TossPaymentCancelRequest;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossBillingChargeResponse;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossBillingIssueResponse;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossErrorResponse;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossPaymentQueryResponse;
import com.keyfeed.keyfeedmonolithic.global.error.exception.InternalApiRequestException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.function.Supplier;

@Slf4j
@Component
public class TossPaymentsClient {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String PATH_BILLING_ISSUE = "/v1/billing/authorizations/issue";
    private static final String PATH_BILLING = "/v1/billing/{billingKey}";
    private static final String PATH_PAYMENT_CANCEL = "/v1/payments/{paymentKey}/cancel";
    private static final String PATH_PAYMENT_BY_ORDER = "/v1/payments/orders/{orderId}";

    private static final String ERR_INVALID_CARD_EXPIRATION = "INVALID_CARD_EXPIRATION";
    private static final String ERR_CARD_PROCESSING_ERROR = "CARD_PROCESSING_ERROR";
    private static final String ERR_EXCEED_INSTALLMENT = "EXCEED_MAX_CARD_INSTALLMENT_PLAN";
    private static final String ERR_UNAUTHORIZED_KEY = "UNAUTHORIZED_KEY";

    private final RestTemplate restTemplate;
    private final TossPaymentsProperties properties;
    private final MeterRegistry meterRegistry;

    public TossPaymentsClient(@Qualifier("tossRestTemplate") RestTemplate restTemplate,
                              TossPaymentsProperties properties,
                              MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    // 빌링키 발급: 카드 등록 완료 후 authKey와 customerKey로 빌링키를 발급받는다
    public TossBillingIssueResponse issueBillingKey(TossBillingIssueRequest request) {
        log.info("[Toss] 빌링키 발급 요청 - customerKey: {}", request.getCustomerKey());
        long start = System.nanoTime();

        URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path(PATH_BILLING_ISSUE)
                .build().toUri();

        try {
            TossBillingIssueResponse response = record("issueBillingKey",
                    () -> exchange(uri, HttpMethod.POST, request, TossBillingIssueResponse.class));
            log.info("[Toss] 빌링키 발급 성공 - customerKey: {}, billingKey: {}, elapsed: {}ms",
                    request.getCustomerKey(), mask(response.getBillingKey()), elapsed(start));
            return response;
        } catch (Exception e) {
            log.error("[Toss] 빌링키 발급 실패 - customerKey: {}, elapsed: {}ms, error: {}",
                    request.getCustomerKey(), elapsed(start), e.getMessage());
            throw e;
        }
    }

    // 결제 실행: 발급된 빌링키로 구독 첫 결제 및 자동결제를 실행한다
    public TossBillingChargeResponse chargeBilling(String billingKey, TossBillingChargeRequest request) {
        log.info("[Toss] 결제 요청 - orderId: {}, amount: {}, billingKey: {}",
                request.getOrderId(), request.getAmount(), mask(billingKey));
        long start = System.nanoTime();

        URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path(PATH_BILLING)
                .buildAndExpand(billingKey)
                .toUri();

        try {
            TossBillingChargeResponse response = record("chargeBilling",
                    () -> exchange(uri, HttpMethod.POST, request, TossBillingChargeResponse.class));
            log.info("[Toss] 결제 성공 - orderId: {}, paymentKey: {}, approvedAt: {}, elapsed: {}ms",
                    request.getOrderId(), response.getPaymentKey(), response.getApprovedAt(), elapsed(start));
            return response;
        } catch (Exception e) {
            log.error("[Toss] 결제 실패 - orderId: {}, amount: {}, elapsed: {}ms, error: {}",
                    request.getOrderId(), request.getAmount(), elapsed(start), e.getMessage());
            throw e;
        }
    }

    // 결제 취소: 결제 건을 취소하고 환불을 처리한다
    public void cancelPayment(String paymentKey, TossPaymentCancelRequest request) {
        log.info("[Toss] 결제 취소 요청 - paymentKey: {}, reason: {}",
                paymentKey, request.getCancelReason());
        long start = System.nanoTime();

        URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path(PATH_PAYMENT_CANCEL)
                .buildAndExpand(paymentKey)
                .toUri();

        try {
            record("cancelPayment", () -> {
                exchange(uri, HttpMethod.POST, request, Void.class);
                return null;
            });
            log.info("[Toss] 결제 취소 성공 - paymentKey: {}, elapsed: {}ms",
                    paymentKey, elapsed(start));
        } catch (Exception e) {
            log.error("[Toss] 결제 취소 실패 - paymentKey: {}, elapsed: {}ms, error: {}",
                    paymentKey, elapsed(start), e.getMessage());
            throw e;
        }
    }

    // 결제 조회: orderId로 결제 건의 실제 처리 상태를 조회한다 (서버 재시작 시 READY 복구용)
    public TossPaymentQueryResponse getPaymentByOrderId(String orderId) {
        log.info("[Toss] 결제 조회 요청 - orderId: {}", orderId);
        long start = System.nanoTime();

        URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path(PATH_PAYMENT_BY_ORDER)
                .buildAndExpand(orderId)
                .toUri();

        try {
            TossPaymentQueryResponse response = record("getPaymentByOrderId",
                    () -> exchange(uri, HttpMethod.GET, null, TossPaymentQueryResponse.class));
            log.info("[Toss] 결제 조회 성공 - orderId: {}, status: {}, elapsed: {}ms",
                    orderId, response.getStatus(), elapsed(start));
            return response;
        } catch (Exception e) {
            log.error("[Toss] 결제 조회 실패 - orderId: {}, elapsed: {}ms, error: {}",
                    orderId, elapsed(start), e.getMessage());
            throw e;
        }
    }

    // 빌링키 삭제: 사용자가 결제 수단을 삭제할 때 빌링키를 만료시킨다
    public void deleteBillingKey(String billingKey) {
        log.info("[Toss] 빌링키 삭제 요청 - billingKey: {}", mask(billingKey));
        long start = System.nanoTime();

        URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path(PATH_BILLING)
                .buildAndExpand(billingKey)
                .toUri();

        try {
            record("deleteBillingKey", () -> {
                exchange(uri, HttpMethod.DELETE, null, Void.class);
                return null;
            });
            log.info("[Toss] 빌링키 삭제 성공 - billingKey: {}, elapsed: {}ms",
                    mask(billingKey), elapsed(start));
        } catch (Exception e) {
            log.error("[Toss] 빌링키 삭제 실패 - billingKey: {}, elapsed: {}ms, error: {}",
                    mask(billingKey), elapsed(start), e.getMessage());
            throw e;
        }
    }

    private <T> T exchange(URI uri, HttpMethod method, Object body, Class<T> responseType) {
        HttpEntity<Object> entity = new HttpEntity<>(body);
        try {
            return restTemplate.exchange(uri, method, entity, responseType).getBody();
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

        log.warn("[Toss] API 에러 - code: {}, message: {}", error.getCode(), error.getMessage());

        switch (error.getCode()) {
            case ERR_INVALID_CARD_EXPIRATION -> throw new InvalidPaymentMethodException();
            case ERR_CARD_PROCESSING_ERROR, ERR_EXCEED_INSTALLMENT -> throw new PaymentFailedException();
            case ERR_UNAUTHORIZED_KEY -> throw new TossAuthException();
            default -> log.warn("[Toss] 처리되지 않은 에러 코드: {}", error.getCode());
        }
    }

    private TossErrorResponse parseError(String body) {
        try {
            return mapper.readValue(body, TossErrorResponse.class);
        } catch (Exception e) {
            log.error("[Toss] 에러 응답 파싱 실패: {}", body);
            return null;
        }
    }

    private <T> T record(String method, Supplier<T> call) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T result = call.get();
            sample.stop(Timer.builder("toss.api.duration")
                    .tag("method", method)
                    .tag("outcome", "success")
                    .register(meterRegistry));
            return result;
        } catch (Exception e) {
            sample.stop(Timer.builder("toss.api.duration")
                    .tag("method", method)
                    .tag("outcome", "failure")
                    .tag("error", e.getClass().getSimpleName())
                    .register(meterRegistry));
            throw e;
        }
    }

    private String mask(String value) {
        if (value == null || value.length() <= 6) {
            return "***";
        }
        return value.substring(0, 6) + "***";
    }

    private long elapsed(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
