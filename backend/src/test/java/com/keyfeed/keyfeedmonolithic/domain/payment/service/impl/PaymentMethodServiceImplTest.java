package com.keyfeed.keyfeedmonolithic.domain.payment.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.UserRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.dto.PaymentMethodRegisterRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.payment.dto.PaymentMethodResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentMethod;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.PaymentMethodType;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.Subscription;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.SubscriptionStatus;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.DuplicatePaymentMethodException;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.PaymentMethodAccessDeniedException;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.PaymentMethodInUseException;
import com.keyfeed.keyfeedmonolithic.domain.payment.exception.PaymentMethodNotFoundException;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.PaymentMethodRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.SubscriptionRepository;
import com.keyfeed.keyfeedmonolithic.global.client.toss.TossPaymentsClient;
import com.keyfeed.keyfeedmonolithic.global.client.toss.dto.response.TossBillingIssueResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceImplTest {

    @InjectMocks
    private PaymentMethodServiceImpl paymentMethodService;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TossPaymentsClient tossPaymentsClient;

    // ===== getOrCreateCustomerKey =====

    @Test
    @DisplayName("customerKey 발급 성공 - 기존 키가 있으면 그대로 반환한다")
    void customerKey_발급_성공_기존키_반환() {
        Long userId = 1L;
        User user = makeUser(userId, "existing-customer-key");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        String result = paymentMethodService.getOrCreateCustomerKey(userId);

        assertThat(result).isEqualTo("existing-customer-key");
    }

    @Test
    @DisplayName("customerKey 발급 성공 - 키가 없으면 UUID를 생성하여 저장하고 반환한다")
    void customerKey_발급_성공_신규_생성() {
        Long userId = 1L;
        User user = makeUser(userId, null);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        String result = paymentMethodService.getOrCreateCustomerKey(userId);

        assertThat(result).isNotNull();
        assertThat(user.getCustomerKey()).isEqualTo(result);
    }

    // ===== registerPaymentMethod =====

    @Test
    @DisplayName("결제 수단 등록 성공 - 첫 번째 카드는 isDefault=true로 설정된다")
    void 결제_수단_등록_성공_첫번째_카드_기본값() {
        Long userId = 1L;
        PaymentMethodRegisterRequestDto dto = makeRegisterDto("auth123");
        User user = makeUser(userId, "cust123");

        TossBillingIssueResponse tossResponse = makeTossIssueResponse("billing_key", "신한", "4330****0000");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tossPaymentsClient.issueBillingKey(any())).willReturn(tossResponse);
        given(paymentMethodRepository.existsByUserIdAndDisplayNumberAndIsActiveTrue(userId, "4330****0000")).willReturn(false);
        given(paymentMethodRepository.countByUserIdAndIsActiveTrue(userId)).willReturn(0L);
        given(paymentMethodRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        PaymentMethodResponseDto result = paymentMethodService.registerPaymentMethod(userId, dto);

        assertThat(result.isDefault()).isTrue();
    }

    @Test
    @DisplayName("결제 수단 등록 성공 - 두 번째 카드는 isDefault=false로 설정된다")
    void 결제_수단_등록_성공_두번째_카드_비기본값() {
        Long userId = 1L;
        PaymentMethodRegisterRequestDto dto = makeRegisterDto("auth123");
        User user = makeUser(userId, "existing_customer_key");

        TossBillingIssueResponse tossResponse = makeTossIssueResponse("billing_key", "현대", "5500****1111");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tossPaymentsClient.issueBillingKey(any())).willReturn(tossResponse);
        given(paymentMethodRepository.existsByUserIdAndDisplayNumberAndIsActiveTrue(userId, "5500****1111")).willReturn(false);
        given(paymentMethodRepository.countByUserIdAndIsActiveTrue(userId)).willReturn(1L);
        given(paymentMethodRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        PaymentMethodResponseDto result = paymentMethodService.registerPaymentMethod(userId, dto);

        assertThat(result.isDefault()).isFalse();
    }

    @Test
    @DisplayName("결제 수단 등록 실패 - 동일 카드 중복 등록 시 DuplicatePaymentMethodException 발생")
    void 결제_수단_등록_실패_중복() {
        Long userId = 1L;
        PaymentMethodRegisterRequestDto dto = makeRegisterDto("auth123");
        User user = makeUser(userId, "cust123");

        TossBillingIssueResponse tossResponse = makeTossIssueResponse("billing_key", "신한", "4330****0000");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tossPaymentsClient.issueBillingKey(any())).willReturn(tossResponse);
        given(paymentMethodRepository.existsByUserIdAndDisplayNumberAndIsActiveTrue(userId, "4330****0000")).willReturn(true);

        assertThatThrownBy(() -> paymentMethodService.registerPaymentMethod(userId, dto))
                .isInstanceOf(DuplicatePaymentMethodException.class);
    }

    // ===== getPaymentMethods =====

    @Test
    @DisplayName("결제 수단 목록 조회 성공 - 활성 수단만 반환한다")
    void 결제_수단_목록_조회_성공() {
        Long userId = 1L;
        User user = makeUser(userId, "cust");
        PaymentMethod pm = makeActivePaymentMethod(1L, user, true);

        given(paymentMethodRepository.findByUserIdAndIsActiveTrue(userId)).willReturn(List.of(pm));

        List<PaymentMethodResponseDto> result = paymentMethodService.getPaymentMethods(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMethodId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("결제 수단 목록 조회 성공 - 빈 목록 반환")
    void 결제_수단_목록_조회_빈목록() {
        Long userId = 1L;
        given(paymentMethodRepository.findByUserIdAndIsActiveTrue(userId)).willReturn(List.of());

        List<PaymentMethodResponseDto> result = paymentMethodService.getPaymentMethods(userId);

        assertThat(result).isEmpty();
    }

    // ===== deletePaymentMethod =====

    @Test
    @DisplayName("결제 수단 삭제 성공 - 소프트 삭제된다")
    void 결제_수단_삭제_성공() {
        Long userId = 1L;
        Long methodId = 10L;
        User user = makeUser(userId, "cust");
        PaymentMethod pm = makeActivePaymentMethod(methodId, user, false);

        given(paymentMethodRepository.findByIdAndIsActiveTrue(methodId)).willReturn(Optional.of(pm));
        given(subscriptionRepository.existsByPaymentMethodIdAndStatus(methodId, SubscriptionStatus.ACTIVE)).willReturn(false);

        paymentMethodService.deletePaymentMethod(userId, methodId);

        assertThat(pm.isActive()).isFalse();
        assertThat(pm.getDeletedAt()).isNotNull();
        then(tossPaymentsClient).should().deleteBillingKey("billing_key_test");
    }

    @Test
    @DisplayName("결제 수단 삭제 실패 - 본인 소유가 아닌 경우 403 예외")
    void 결제_수단_삭제_실패_권한없음() {
        Long userId = 1L;
        Long otherUserId = 2L;
        Long methodId = 10L;
        User owner = makeUser(otherUserId, "cust");
        PaymentMethod pm = makeActivePaymentMethod(methodId, owner, false);

        given(paymentMethodRepository.findByIdAndIsActiveTrue(methodId)).willReturn(Optional.of(pm));

        assertThatThrownBy(() -> paymentMethodService.deletePaymentMethod(userId, methodId))
                .isInstanceOf(PaymentMethodAccessDeniedException.class);
    }

    @Test
    @DisplayName("결제 수단 삭제 실패 - 구독에 연결된 수단은 409 예외")
    void 결제_수단_삭제_실패_구독연결됨() {
        Long userId = 1L;
        Long methodId = 10L;
        User user = makeUser(userId, "cust");
        PaymentMethod pm = makeActivePaymentMethod(methodId, user, false);

        given(paymentMethodRepository.findByIdAndIsActiveTrue(methodId)).willReturn(Optional.of(pm));
        given(subscriptionRepository.existsByPaymentMethodIdAndStatus(methodId, SubscriptionStatus.ACTIVE)).willReturn(true);

        assertThatThrownBy(() -> paymentMethodService.deletePaymentMethod(userId, methodId))
                .isInstanceOf(PaymentMethodInUseException.class);
    }

    @Test
    @DisplayName("결제 수단 삭제 실패 - 존재하지 않는 수단은 404 예외")
    void 결제_수단_삭제_실패_없는수단() {
        Long userId = 1L;
        Long methodId = 99L;
        given(paymentMethodRepository.findByIdAndIsActiveTrue(methodId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentMethodService.deletePaymentMethod(userId, methodId))
                .isInstanceOf(PaymentMethodNotFoundException.class);
    }

    @Test
    @DisplayName("기본 수단 삭제 시 차순위 수단이 자동으로 기본 수단으로 지정된다")
    void 기본_수단_삭제_후_차순위_자동지정() {
        Long userId = 1L;
        Long methodId = 10L;
        User user = makeUser(userId, "cust");
        PaymentMethod defaultPm = makeActivePaymentMethod(methodId, user, true);
        PaymentMethod nextPm = makeActivePaymentMethod(20L, user, false);

        given(paymentMethodRepository.findByIdAndIsActiveTrue(methodId)).willReturn(Optional.of(defaultPm));
        given(subscriptionRepository.existsByPaymentMethodIdAndStatus(methodId, SubscriptionStatus.ACTIVE)).willReturn(false);
        given(paymentMethodRepository.findTopByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId)).willReturn(Optional.of(nextPm));

        paymentMethodService.deletePaymentMethod(userId, methodId);

        assertThat(nextPm.isDefault()).isTrue();
    }

    // ===== changeDefaultPaymentMethod =====

    @Test
    @DisplayName("기본 결제 수단 변경 성공")
    void 기본_결제_수단_변경_성공() {
        Long userId = 1L;
        Long methodId = 20L;
        User user = makeUser(userId, "cust");
        PaymentMethod currentDefault = makeActivePaymentMethod(10L, user, true);
        PaymentMethod newDefault = makeActivePaymentMethod(methodId, user, false);

        given(paymentMethodRepository.findByIdAndIsActiveTrue(methodId)).willReturn(Optional.of(newDefault));
        given(paymentMethodRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId)).willReturn(Optional.of(currentDefault));
        given(subscriptionRepository.findByUserIdAndStatusIn(eq(userId), any())).willReturn(List.of());

        paymentMethodService.changeDefaultPaymentMethod(userId, methodId);

        assertThat(currentDefault.isDefault()).isFalse();
        assertThat(newDefault.isDefault()).isTrue();
    }

    @Test
    @DisplayName("이미 기본 수단인 경우 멱등성 보장 - 변경 없이 반환")
    void 기본_결제_수단_변경_멱등성() {
        Long userId = 1L;
        Long methodId = 10L;
        User user = makeUser(userId, "cust");
        PaymentMethod alreadyDefault = makeActivePaymentMethod(methodId, user, true);

        given(paymentMethodRepository.findByIdAndIsActiveTrue(methodId)).willReturn(Optional.of(alreadyDefault));

        paymentMethodService.changeDefaultPaymentMethod(userId, methodId);

        then(paymentMethodRepository).should(never()).findByUserIdAndIsDefaultTrueAndIsActiveTrue(any());
    }

    @Test
    @DisplayName("기본 수단 변경 시 PAUSED 구독이 ACTIVE로 복구된다")
    void 기본_수단_변경_PAUSED_구독_복구() {
        Long userId = 1L;
        Long methodId = 20L;
        User user = makeUser(userId, "cust");
        PaymentMethod newDefault = makeActivePaymentMethod(methodId, user, false);
        Subscription pausedSub = Subscription.builder()
                .user(user)
                .paymentMethod(newDefault)
                .status(SubscriptionStatus.PAUSED)
                .retryCount(3)
                .build();

        given(paymentMethodRepository.findByIdAndIsActiveTrue(methodId)).willReturn(Optional.of(newDefault));
        given(paymentMethodRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId)).willReturn(Optional.empty());
        given(subscriptionRepository.findByUserIdAndStatusIn(eq(userId), any())).willReturn(List.of(pausedSub));

        paymentMethodService.changeDefaultPaymentMethod(userId, methodId);

        assertThat(pausedSub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(pausedSub.getRetryCount()).isZero();
    }

    // ===== helpers =====

    private PaymentMethodRegisterRequestDto makeRegisterDto(String authKey) {
        try {
            var dto = new PaymentMethodRegisterRequestDto();
            var authKeyField = PaymentMethodRegisterRequestDto.class.getDeclaredField("authKey");
            authKeyField.setAccessible(true);
            authKeyField.set(dto, authKey);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private User makeUser(Long id, String customerKey) {
        return User.builder()
                .id(id)
                .email("test@test.com")
                .customerKey(customerKey)
                .build();
    }

    private PaymentMethod makeActivePaymentMethod(Long id, User user, boolean isDefault) {
        return PaymentMethod.builder()
                .id(id)
                .user(user)
                .billingKey("billing_key_test")
                .methodType(PaymentMethodType.CARD)
                .providerName("신한")
                .displayNumber("4330****0000")
                .isDefault(isDefault)
                .isActive(true)
                .build();
    }

    private TossBillingIssueResponse makeTossIssueResponse(String billingKey, String cardCompany, String cardNumber) {
        try {
            var response = new TossBillingIssueResponse();
            setField(response, "billingKey", billingKey);
            setField(response, "cardCompany", cardCompany);
            setField(response, "method", "카드");

            var cardClass = TossBillingIssueResponse.Card.class;
            var card = cardClass.getDeclaredConstructor().newInstance();
            setField(card, "number", cardNumber);
            setField(response, "card", card);

            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
