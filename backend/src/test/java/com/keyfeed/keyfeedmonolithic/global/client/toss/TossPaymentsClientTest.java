package com.keyfeed.keyfeedmonolithic.global.client.toss;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.InvalidPaymentMethodException;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.PaymentFailedException;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.TossAuthException;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.request.TossBillingChargeRequest;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.request.TossBillingIssueRequest;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossBillingChargeResponse;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossBillingIssueResponse;
import com.keyfeed.keyfeedmonolithic.global.error.exception.InternalApiRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

@WireMockTest
class TossPaymentsClientTest {

    private TossPaymentsClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        TossPaymentsProperties properties = new TossPaymentsProperties();
        properties.setSecretKey("test_sk_dummy_key");
        properties.setBaseUrl(wmInfo.getHttpBaseUrl());

        TossPaymentsConfig config = new TossPaymentsConfig(properties);
        RestTemplate restTemplate = config.tossRestTemplate();

        client = new TossPaymentsClient(restTemplate, properties);
    }

    // ===== 빌링키 발급 =====

    @Test
    @DisplayName("빌링키 발급 성공 - 200 응답을 TossBillingIssueResponse로 파싱한다")
    void issueBillingKey_success() {
        stubFor(post(urlEqualTo("/v1/billing/authorizations/issue"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "billingKey": "billing_abc123",
                                  "customerKey": "user_001",
                                  "method": "카드",
                                  "cardCompany": "신한",
                                  "card": { "number": "4330000000000000", "cardType": "신용" }
                                }
                                """)));

        TossBillingIssueRequest request = TossBillingIssueRequest.builder()
                .authKey("auth_key_test")
                .customerKey("user_001")
                .build();

        TossBillingIssueResponse response = client.issueBillingKey(request);

        assertThat(response.getBillingKey()).isEqualTo("billing_abc123");
        assertThat(response.getMethod()).isEqualTo("카드");
        assertThat(response.getCardCompany()).isEqualTo("신한");
        assertThat(response.getCard().getNumber()).isEqualTo("4330000000000000");
    }

    @Test
    @DisplayName("빌링키 발급 실패 - INVALID_CARD_EXPIRATION → InvalidPaymentMethodException")
    void issueBillingKey_invalidCardExpiration() {
        stubFor(post(urlEqualTo("/v1/billing/authorizations/issue"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"code": "INVALID_CARD_EXPIRATION", "message": "카드 유효기간 오류입니다."}
                                """)));

        TossBillingIssueRequest request = TossBillingIssueRequest.builder()
                .authKey("auth_key_test")
                .customerKey("user_001")
                .build();

        assertThatThrownBy(() -> client.issueBillingKey(request))
                .isInstanceOf(InvalidPaymentMethodException.class);
    }

    @Test
    @DisplayName("빌링키 발급 실패 - UNAUTHORIZED_KEY → TossAuthException")
    void issueBillingKey_unauthorizedKey() {
        stubFor(post(urlEqualTo("/v1/billing/authorizations/issue"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"code": "UNAUTHORIZED_KEY", "message": "인증되지 않은 시크릿 키입니다."}
                                """)));

        TossBillingIssueRequest request = TossBillingIssueRequest.builder()
                .authKey("invalid_key")
                .customerKey("user_001")
                .build();

        assertThatThrownBy(() -> client.issueBillingKey(request))
                .isInstanceOf(TossAuthException.class);
    }

    // ===== 결제 실행 =====

    @Test
    @DisplayName("결제 실행 성공 - 200 응답, status=DONE 확인")
    void chargeBilling_success() {
        stubFor(post(urlMatching("/v1/billing/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "paymentKey": "pay_key_xyz",
                                  "orderId": "order_001",
                                  "orderName": "프리미엄 구독 1개월",
                                  "status": "DONE",
                                  "totalAmount": 9900,
                                  "approvedAt": "2026-04-01T12:00:00+09:00"
                                }
                                """)));

        TossBillingChargeRequest request = TossBillingChargeRequest.builder()
                .customerKey("user_001")
                .amount(9900)
                .orderId("order_001")
                .orderName("프리미엄 구독 1개월")
                .build();

        TossBillingChargeResponse response = client.chargeBilling("billing_abc123", request);

        assertThat(response.getPaymentKey()).isEqualTo("pay_key_xyz");
        assertThat(response.getStatus()).isEqualTo("DONE");
        assertThat(response.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("결제 실행 실패 - CARD_PROCESSING_ERROR → PaymentFailedException")
    void chargeBilling_cardProcessingError() {
        stubFor(post(urlMatching("/v1/billing/.*"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"code": "CARD_PROCESSING_ERROR", "message": "카드사에서 처리 중 오류가 발생했습니다."}
                                """)));

        TossBillingChargeRequest request = TossBillingChargeRequest.builder()
                .customerKey("user_001")
                .amount(9900)
                .orderId("order_001")
                .orderName("프리미엄 구독 1개월")
                .build();

        assertThatThrownBy(() -> client.chargeBilling("billing_abc123", request))
                .isInstanceOf(PaymentFailedException.class);
    }

    @Test
    @DisplayName("결제 실행 실패 - EXCEED_MAX_CARD_INSTALLMENT_PLAN → PaymentFailedException")
    void chargeBilling_exceedInstallment() {
        stubFor(post(urlMatching("/v1/billing/.*"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"code": "EXCEED_MAX_CARD_INSTALLMENT_PLAN", "message": "할부 한도를 초과했습니다."}
                                """)));

        TossBillingChargeRequest request = TossBillingChargeRequest.builder()
                .customerKey("user_001")
                .amount(9900)
                .orderId("order_001")
                .orderName("프리미엄 구독 1개월")
                .build();

        assertThatThrownBy(() -> client.chargeBilling("billing_abc123", request))
                .isInstanceOf(PaymentFailedException.class);
    }

    @Test
    @DisplayName("결제 실행 실패 - 네트워크 타임아웃 → InternalApiRequestException")
    void chargeBilling_networkTimeout() {
        stubFor(post(urlMatching("/v1/billing/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(11000)));  // readTimeout(10s) 초과

        TossBillingChargeRequest request = TossBillingChargeRequest.builder()
                .customerKey("user_001")
                .amount(9900)
                .orderId("order_001")
                .orderName("프리미엄 구독 1개월")
                .build();

        assertThatThrownBy(() -> client.chargeBilling("billing_abc123", request))
                .isInstanceOf(InternalApiRequestException.class);
    }

    // ===== 빌링키 삭제 =====

    @Test
    @DisplayName("빌링키 삭제 성공 - 200 응답, 예외 없음")
    void deleteBillingKey_success() {
        stubFor(delete(urlMatching("/v1/billing/.*"))
                .willReturn(aResponse()
                        .withStatus(200)));

        assertThatCode(() -> client.deleteBillingKey("billing_abc123"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("빌링키 삭제 실패 - 404 미처리 코드 → InternalApiRequestException")
    void deleteBillingKey_notFound() {
        stubFor(delete(urlMatching("/v1/billing/.*"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"code": "NOT_FOUND_BILLING_KEY", "message": "존재하지 않는 빌링키입니다."}
                                """)));

        assertThatThrownBy(() -> client.deleteBillingKey("invalid_billing_key"))
                .isInstanceOf(InternalApiRequestException.class);
    }

    // ===== 에러 처리 =====

    @Test
    @DisplayName("알 수 없는 에러 코드 - InternalApiRequestException fallback")
    void handleClientError_unknownCode() {
        stubFor(post(urlEqualTo("/v1/billing/authorizations/issue"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"code": "UNKNOWN_ERROR_CODE", "message": "알 수 없는 오류입니다."}
                                """)));

        TossBillingIssueRequest request = TossBillingIssueRequest.builder()
                .authKey("auth_key_test")
                .customerKey("user_001")
                .build();

        assertThatThrownBy(() -> client.issueBillingKey(request))
                .isInstanceOf(InternalApiRequestException.class);
    }

    @Test
    @DisplayName("요청 헤더 검증 - Authorization Basic 헤더 포함 확인")
    void authorizationHeader_isBasicEncoded() {
        stubFor(post(urlEqualTo("/v1/billing/authorizations/issue"))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Basic .+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"billingKey": "billing_abc123", "customerKey": "user_001", "method": "카드"}
                                """)));

        TossBillingIssueRequest request = TossBillingIssueRequest.builder()
                .authKey("auth_key_test")
                .customerKey("user_001")
                .build();

        client.issueBillingKey(request);

        verify(postRequestedFor(urlEqualTo("/v1/billing/authorizations/issue"))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Basic .+")));
    }
}
