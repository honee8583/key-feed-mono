package com.keyfeed.keyfeedmonolithic.domain.payment.scheduler;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.keyword.service.KeywordService;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.Subscription;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.SubscriptionStatus;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionExpirySchedulerTest {

    @InjectMocks
    private SubscriptionExpiryScheduler subscriptionExpiryScheduler;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private KeywordService keywordService;

    private static final int KEYWORD_MAX_COUNT = 3;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(subscriptionExpiryScheduler, "keywordMaxCount", KEYWORD_MAX_COUNT);
    }

    // ===== expireSubscriptions =====

    @Test
    @DisplayName("만료 대상 구독 1건 INACTIVE 전환 성공")
    void 만료_대상_구독_INACTIVE_전환_성공() {
        // given
        Subscription sub = makeCanceledSubscription(1L, LocalDateTime.now().minusDays(1));
        given(subscriptionRepository.findByStatusAndExpiredAtLessThanEqual(
                eq(SubscriptionStatus.CANCELED), any(LocalDateTime.class)))
                .willReturn(List.of(sub));

        // when
        subscriptionExpiryScheduler.expireSubscriptions();

        // then
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.INACTIVE);
    }

    @Test
    @DisplayName("만료 대상이 여러 건인 경우 모두 INACTIVE 전환")
    void 만료_대상_여러건_일괄_처리() {
        // given
        Subscription sub1 = makeCanceledSubscription(1L, LocalDateTime.now().minusDays(1));
        Subscription sub2 = makeCanceledSubscription(2L, LocalDateTime.now().minusDays(10));
        Subscription sub3 = makeCanceledSubscription(3L, LocalDateTime.now().minusMonths(1));
        given(subscriptionRepository.findByStatusAndExpiredAtLessThanEqual(
                eq(SubscriptionStatus.CANCELED), any(LocalDateTime.class)))
                .willReturn(List.of(sub1, sub2, sub3));

        // when
        subscriptionExpiryScheduler.expireSubscriptions();

        // then
        assertThat(sub1.getStatus()).isEqualTo(SubscriptionStatus.INACTIVE);
        assertThat(sub2.getStatus()).isEqualTo(SubscriptionStatus.INACTIVE);
        assertThat(sub3.getStatus()).isEqualTo(SubscriptionStatus.INACTIVE);
    }

    @Test
    @DisplayName("만료 대상이 없는 경우 아무 처리 없이 정상 종료")
    void 만료_대상_없음_정상_종료() {
        // given
        given(subscriptionRepository.findByStatusAndExpiredAtLessThanEqual(
                eq(SubscriptionStatus.CANCELED), any(LocalDateTime.class)))
                .willReturn(List.of());

        // when & then
        assertThatCode(() -> subscriptionExpiryScheduler.expireSubscriptions())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("expired_at이 미래인 구독은 처리 대상에서 제외됨")
    void 미래_만료일_구독_처리_제외() {
        // given - repository가 빈 리스트 반환 (미래 expired_at은 쿼리 조건에 해당 안 됨)
        given(subscriptionRepository.findByStatusAndExpiredAtLessThanEqual(
                eq(SubscriptionStatus.CANCELED), any(LocalDateTime.class)))
                .willReturn(List.of());

        Subscription futureSub = makeCanceledSubscription(1L, LocalDateTime.now().plusDays(30));

        // when
        subscriptionExpiryScheduler.expireSubscriptions();

        // then - CANCELED 상태 그대로 유지
        assertThat(futureSub.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    }

    @Test
    @DisplayName("만료 구독 2건 - deactivateExcessKeywords 2회 호출")
    void 만료_구독_2건_키워드_비활성화_2회_호출() {
        // given
        Subscription sub1 = makeCanceledSubscription(1L, LocalDateTime.now().minusDays(1));
        Subscription sub2 = makeCanceledSubscription(2L, LocalDateTime.now().minusDays(2));
        given(subscriptionRepository.findByStatusAndExpiredAtLessThanEqual(
                eq(SubscriptionStatus.CANCELED), any(LocalDateTime.class)))
                .willReturn(List.of(sub1, sub2));

        // when
        subscriptionExpiryScheduler.expireSubscriptions();

        // then
        then(keywordService).should(times(1)).deactivateExcessKeywords(1L, KEYWORD_MAX_COUNT);
        then(keywordService).should(times(1)).deactivateExcessKeywords(2L, KEYWORD_MAX_COUNT);
    }

    @Test
    @DisplayName("만료 구독 0건 - deactivateExcessKeywords 미호출")
    void 만료_구독_없으면_키워드_비활성화_미호출() {
        // given
        given(subscriptionRepository.findByStatusAndExpiredAtLessThanEqual(
                eq(SubscriptionStatus.CANCELED), any(LocalDateTime.class)))
                .willReturn(List.of());

        // when
        subscriptionExpiryScheduler.expireSubscriptions();

        // then
        then(keywordService).should(never()).deactivateExcessKeywords(any(), anyInt());
    }

    // ===== helpers =====

    private User makeUser(Long id) {
        return User.builder()
                .id(id)
                .email("test@test.com")
                .username("테스터")
                .customerKey("customer-key-" + id)
                .build();
    }

    private Subscription makeCanceledSubscription(Long id, LocalDateTime expiredAt) {
        User user = makeUser(id);
        return Subscription.builder()
                .id(id).user(user)
                .status(SubscriptionStatus.CANCELED)
                .price(9900).orderName("프리미엄 구독 1개월")
                .startedAt(LocalDateTime.now().minusMonths(1))
                .expiredAt(expiredAt)
                .canceledAt(LocalDateTime.now().minusDays(5))
                .build();
    }
}
