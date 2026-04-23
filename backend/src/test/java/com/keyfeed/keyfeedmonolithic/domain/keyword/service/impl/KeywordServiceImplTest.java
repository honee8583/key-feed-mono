package com.keyfeed.keyfeedmonolithic.domain.keyword.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.UserRepository;
import com.keyfeed.keyfeedmonolithic.domain.keyword.dto.KeywordResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.keyword.entity.Keyword;
import com.keyfeed.keyfeedmonolithic.domain.keyword.exception.KeywordLimitExceededException;
import com.keyfeed.keyfeedmonolithic.domain.keyword.repository.KeywordCacheRepository;
import com.keyfeed.keyfeedmonolithic.domain.keyword.repository.KeywordRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.SubscriptionStatus;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.SubscriptionRepository;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityAlreadyExistsException;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class KeywordServiceImplTest {

    @InjectMocks
    private KeywordServiceImpl keywordService;

    @Mock
    private KeywordRepository keywordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private KeywordCacheRepository keywordCacheRepository;

    private static final int KEYWORD_MAX_COUNT = 3;
    private static final int KEYWORD_SUBSCRIBER_MAX_COUNT = 10;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keywordService, "keywordMaxCount", KEYWORD_MAX_COUNT);
        ReflectionTestUtils.setField(keywordService, "keywordSubscriberMaxCount", KEYWORD_SUBSCRIBER_MAX_COUNT);
    }

    private User makeUser(Long id) {
        return User.builder()
                .email("test@test.com")
                .username("testUser")
                .build();
    }

    private Keyword makeKeyword(Long id, User user, String name) {
        return Keyword.builder()
                .user(user)
                .name(name)
                .build();
    }

    // ── 비구독자 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("비구독자 - 한도 미만이면 키워드 추가 성공")
    void 비구독자_한도_미만_키워드_추가_성공() {
        // given
        Long userId = 1L;
        String keywordName = "스프링";
        User user = makeUser(userId);
        Keyword keyword = makeKeyword(1L, user, keywordName);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(false);
        given(keywordRepository.countByUserIdAndIsEnabledTrue(userId)).willReturn(2L);
        given(keywordRepository.save(any(Keyword.class))).willReturn(keyword);

        // when
        KeywordResponseDto result = keywordService.addKeyword(userId, keywordName);

        // then
        assertThat(result.getName()).isEqualTo(keywordName);
        then(keywordRepository).should(times(1)).save(any(Keyword.class));
        then(keywordCacheRepository).should(times(1)).addUserToKeyword(eq(keywordName), eq(userId));
    }

    @Test
    @DisplayName("비구독자 - 한도보다 1개 적을 때 키워드 추가 성공")
    void 비구독자_한도보다_1개_적을때_성공() {
        // given
        Long userId = 2L;
        String keywordName = "도커";
        User user = makeUser(userId);
        Keyword keyword = makeKeyword(1L, user, keywordName);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(false);
        given(keywordRepository.countByUserIdAndIsEnabledTrue(userId)).willReturn((long) KEYWORD_MAX_COUNT - 1);
        given(keywordRepository.save(any(Keyword.class))).willReturn(keyword);

        // when
        KeywordResponseDto result = keywordService.addKeyword(userId, keywordName);

        // then
        assertThat(result.getName()).isEqualTo(keywordName);
        then(keywordCacheRepository).should(times(1)).addUserToKeyword(eq(keywordName), eq(userId));
    }

    @Test
    @DisplayName("비구독자 - 정확히 한도(3개)에 도달하면 KeywordLimitExceededException 발생")
    void 비구독자_정확히_한도_도달_예외_발생() {
        // given
        Long userId = 3L;
        String keywordName = "파이썬";
        User user = makeUser(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(false);
        given(keywordRepository.countByUserIdAndIsEnabledTrue(userId)).willReturn((long) KEYWORD_MAX_COUNT);

        // when & then
        assertThatThrownBy(() -> keywordService.addKeyword(userId, keywordName))
                .isInstanceOf(KeywordLimitExceededException.class)
                .hasMessageContaining("최대 10개까지 이용하실 수 있습니다");

        then(keywordRepository).should(never()).save(any(Keyword.class));
        then(keywordCacheRepository).should(never()).addUserToKeyword(any(), any());
    }

    @Test
    @DisplayName("비구독자 - 한도(3개) 초과 시 KeywordLimitExceededException 발생")
    void 비구독자_한도_초과_예외_발생() {
        // given
        Long userId = 4L;
        String keywordName = "자바";
        User user = makeUser(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(false);
        given(keywordRepository.countByUserIdAndIsEnabledTrue(userId)).willReturn(5L);

        // when & then
        assertThatThrownBy(() -> keywordService.addKeyword(userId, keywordName))
                .isInstanceOf(KeywordLimitExceededException.class);

        then(keywordRepository).should(never()).save(any(Keyword.class));
        then(keywordCacheRepository).should(never()).addUserToKeyword(any(), any());
    }

    // ── 구독자(ACTIVE) ─────────────────────────────────────────────────

    @Test
    @DisplayName("구독자(ACTIVE) - 비구독 한도(3개) 초과해도 구독자 한도 내이면 키워드 추가 성공")
    void 구독자_비구독_한도_초과해도_구독자_한도_내_추가_성공() {
        // given
        Long userId = 5L;
        String keywordName = "코틀린";
        User user = makeUser(userId);
        Keyword keyword = makeKeyword(1L, user, keywordName);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(true);
        given(keywordRepository.countByUserIdAndIsEnabledTrue(userId)).willReturn(9L);
        given(keywordRepository.save(any(Keyword.class))).willReturn(keyword);

        // when
        KeywordResponseDto result = keywordService.addKeyword(userId, keywordName);

        // then
        assertThat(result.getName()).isEqualTo(keywordName);
        then(keywordRepository).should(times(1)).save(any(Keyword.class));
        then(keywordCacheRepository).should(times(1)).addUserToKeyword(eq(keywordName), eq(userId));
    }

    @Test
    @DisplayName("구독자(ACTIVE) - 구독자 한도(10개)에 도달하면 구독자용 메시지로 예외 발생")
    void 구독자_한도_도달_예외_발생() {
        // given
        Long userId = 6L;
        String keywordName = "리액트";
        User user = makeUser(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(true);
        given(keywordRepository.countByUserIdAndIsEnabledTrue(userId)).willReturn((long) KEYWORD_SUBSCRIBER_MAX_COUNT);

        // when & then
        assertThatThrownBy(() -> keywordService.addKeyword(userId, keywordName))
                .isInstanceOf(KeywordLimitExceededException.class)
                .hasMessageContaining("키워드 등록 한도(10개)에 도달했습니다");

        then(keywordRepository).should(never()).save(any(Keyword.class));
        then(keywordCacheRepository).should(never()).addUserToKeyword(any(), any());
    }

    @Test
    @DisplayName("구독자(ACTIVE) - 구독자 한도(10개) 미만이면 키워드 추가 성공")
    void 구독자_한도_미만_키워드_추가_성공() {
        // given
        Long userId = 7L;
        String keywordName = "쿠버네티스";
        User user = makeUser(userId);
        Keyword keyword = makeKeyword(1L, user, keywordName);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(true);
        given(keywordRepository.countByUserIdAndIsEnabledTrue(userId)).willReturn((long) KEYWORD_SUBSCRIBER_MAX_COUNT - 1);
        given(keywordRepository.save(any(Keyword.class))).willReturn(keyword);

        // when
        KeywordResponseDto result = keywordService.addKeyword(userId, keywordName);

        // then
        assertThat(result.getName()).isEqualTo(keywordName);
        then(keywordRepository).should(times(1)).save(any(Keyword.class));
        then(keywordCacheRepository).should(times(1)).addUserToKeyword(eq(keywordName), eq(userId));
    }

    // ── 구독자(CANCELED, 만료 전) ─────────────────────────────────────

    @Test
    @DisplayName("구독자(CANCELED, 만료 전) - 구독자 한도 내이면 키워드 추가 성공")
    void 구독취소_만료전_한도_내_키워드_추가_성공() {
        // given
        Long userId = 8L;
        String keywordName = "그래들";
        User user = makeUser(userId);
        Keyword keyword = makeKeyword(1L, user, keywordName);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(true);
        given(keywordRepository.countByUserIdAndIsEnabledTrue(userId)).willReturn(5L);
        given(keywordRepository.save(any(Keyword.class))).willReturn(keyword);

        // when
        KeywordResponseDto result = keywordService.addKeyword(userId, keywordName);

        // then
        assertThat(result.getName()).isEqualTo(keywordName);
        then(keywordRepository).should(times(1)).save(any(Keyword.class));
        then(keywordCacheRepository).should(times(1)).addUserToKeyword(eq(keywordName), eq(userId));
    }

    @Test
    @DisplayName("구독자(CANCELED, 만료 전) - 구독자 한도(10개) 초과 시 KeywordLimitExceededException 발생")
    void 구독취소_만료전_한도_초과_예외_발생() {
        // given
        Long userId = 9L;
        String keywordName = "넥스트";
        User user = makeUser(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(true);
        given(keywordRepository.countByUserIdAndIsEnabledTrue(userId)).willReturn(10L);

        // when & then
        assertThatThrownBy(() -> keywordService.addKeyword(userId, keywordName))
                .isInstanceOf(KeywordLimitExceededException.class);

        then(keywordRepository).should(never()).save(any(Keyword.class));
        then(keywordCacheRepository).should(never()).addUserToKeyword(any(), any());
    }

    // ── 공통 예외 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("중복 키워드 등록 시 EntityAlreadyExistsException 발생")
    void 중복_키워드_등록_예외_발생() {
        // given
        Long userId = 10L;
        String keywordName = "중복키워드";
        User user = makeUser(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> keywordService.addKeyword(userId, keywordName))
                .isInstanceOf(EntityAlreadyExistsException.class);

        then(subscriptionRepository).should(never()).existsByUserIdAndStatusIn(any(), any());
        then(keywordRepository).should(never()).save(any(Keyword.class));
        then(keywordCacheRepository).should(never()).addUserToKeyword(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 - EntityNotFoundException 발생")
    void 존재하지_않는_사용자_예외_발생() {
        // given
        Long userId = 999L;

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> keywordService.addKeyword(userId, "키워드"))
                .isInstanceOf(EntityNotFoundException.class);

        then(keywordRepository).should(never()).save(any(Keyword.class));
        then(keywordCacheRepository).should(never()).addUserToKeyword(any(), any());
    }

    // ── Redis 동기화 ───────────────────────────────────────────────────

    @Test
    @DisplayName("키워드 등록 시 Redis SADD 호출")
    void 키워드_등록시_Redis_SADD_호출() {
        // given
        Long userId = 20L;
        String keywordName = "레디스";
        User user = makeUser(userId);
        Keyword keyword = makeKeyword(1L, user, keywordName);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(false);
        given(keywordRepository.countByUserIdAndIsEnabledTrue(userId)).willReturn(1L);
        given(keywordRepository.save(any(Keyword.class))).willReturn(keyword);

        // when
        keywordService.addKeyword(userId, keywordName);

        // then
        then(keywordCacheRepository).should(times(1)).addUserToKeyword(eq(keywordName), eq(userId));
    }

    @Test
    @DisplayName("키워드 삭제 시 Redis SREM 호출")
    void 키워드_삭제시_Redis_SREM_호출() {
        // given
        Long userId = 21L;
        Long keywordId = 100L;
        String keywordName = "삭제키워드";
        User user = makeUser(userId);
        Keyword keyword = makeKeyword(keywordId, user, keywordName);

        given(keywordRepository.findByIdAndUserId(keywordId, userId)).willReturn(Optional.of(keyword));

        // when
        keywordService.deleteKeyword(userId, keywordId);

        // then
        then(keywordRepository).should(times(1)).delete(keyword);
        then(keywordCacheRepository).should(times(1)).removeUserFromKeyword(eq(keywordName), eq(userId));
    }

    @Test
    @DisplayName("존재하지 않는 키워드 삭제 시 EntityNotFoundException 발생, Redis 미호출")
    void 존재하지않는_키워드_삭제시_예외_발생_Redis_미호출() {
        // given
        Long userId = 22L;
        Long keywordId = 999L;

        given(keywordRepository.findByIdAndUserId(keywordId, userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> keywordService.deleteKeyword(userId, keywordId))
                .isInstanceOf(EntityNotFoundException.class);

        then(keywordCacheRepository).should(never()).removeUserFromKeyword(any(), any());
    }

    @Test
    @DisplayName("알림 ON 전환 시 Redis SADD 호출")
    void 알림_ON_전환시_Redis_SADD_호출() {
        // given
        Long userId = 23L;
        Long keywordId = 200L;
        String keywordName = "토글온";
        User user = makeUser(userId);
        Keyword keyword = Keyword.builder()
                .user(user)
                .name(keywordName)
                .isNotificationEnabled(false)
                .build();

        given(keywordRepository.findByIdAndUserId(keywordId, userId)).willReturn(Optional.of(keyword));
        given(keywordRepository.save(any(Keyword.class))).willReturn(keyword);

        // when
        keywordService.toggleKeywordNotification(userId, keywordId);

        // then: false → true 로 토글되었으므로 SADD
        then(keywordCacheRepository).should(times(1)).addUserToKeyword(eq(keywordName), eq(userId));
        then(keywordCacheRepository).should(never()).removeUserFromKeyword(any(), any());
    }

    @Test
    @DisplayName("알림 OFF 전환 시 Redis SREM 호출")
    void 알림_OFF_전환시_Redis_SREM_호출() {
        // given
        Long userId = 24L;
        Long keywordId = 201L;
        String keywordName = "토글오프";
        User user = makeUser(userId);
        Keyword keyword = makeKeyword(keywordId, user, keywordName); // isNotificationEnabled = true

        given(keywordRepository.findByIdAndUserId(keywordId, userId)).willReturn(Optional.of(keyword));
        given(keywordRepository.save(any(Keyword.class))).willReturn(keyword);

        // when
        keywordService.toggleKeywordNotification(userId, keywordId);

        // then: true → false 로 토글되었으므로 SREM
        then(keywordCacheRepository).should(times(1)).removeUserFromKeyword(eq(keywordName), eq(userId));
        then(keywordCacheRepository).should(never()).addUserToKeyword(any(), any());
    }

    // ── deactivateExcessKeywords ───────────────────────────────────────

    @Test
    @DisplayName("비활성화 - 5개 키워드 중 앞 3개 유지, 나머지 2개 비활성화")
    void 초과_키워드_비활성화_성공() {
        // given
        Long userId = 11L;
        User user = makeUser(userId);
        List<Keyword> keywords = List.of(
                makeKeyword(1L, user, "키워드1"),
                makeKeyword(2L, user, "키워드2"),
                makeKeyword(3L, user, "키워드3"),
                makeKeyword(4L, user, "키워드4"),
                makeKeyword(5L, user, "키워드5")
        );

        given(keywordRepository.findByUserIdOrderByCreatedAtAsc(userId)).willReturn(keywords);

        // when
        keywordService.deactivateExcessKeywords(userId, 3);

        // then
        assertThat(keywords.get(0).isEnabled()).isTrue();
        assertThat(keywords.get(1).isEnabled()).isTrue();
        assertThat(keywords.get(2).isEnabled()).isTrue();
        assertThat(keywords.get(3).isEnabled()).isFalse();
        assertThat(keywords.get(4).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("비활성화 - 키워드 수가 keepCount 미만이면 비활성화 없음")
    void 키워드_수_한도_미만이면_비활성화_없음() {
        // given
        Long userId = 12L;
        User user = makeUser(userId);
        List<Keyword> keywords = List.of(
                makeKeyword(1L, user, "키워드1"),
                makeKeyword(2L, user, "키워드2")
        );

        given(keywordRepository.findByUserIdOrderByCreatedAtAsc(userId)).willReturn(keywords);

        // when
        keywordService.deactivateExcessKeywords(userId, 3);

        // then
        assertThat(keywords.get(0).isEnabled()).isTrue();
        assertThat(keywords.get(1).isEnabled()).isTrue();
    }

    @Test
    @DisplayName("비활성화 - 키워드가 없으면 아무것도 하지 않음")
    void 키워드_없으면_비활성화_없음() {
        // given
        Long userId = 13L;

        given(keywordRepository.findByUserIdOrderByCreatedAtAsc(userId)).willReturn(List.of());

        // when
        keywordService.deactivateExcessKeywords(userId, 3);

        // then: 예외 없이 정상 종료
    }

    // ── reactivateAllKeywords ─────────────────────────────────────────

    @Test
    @DisplayName("재활성화 - enableAllByUserId 호출 검증")
    void 비활성화된_키워드_전체_복원() {
        // given
        Long userId = 14L;

        // when
        keywordService.reactivateAllKeywords(userId);

        // then
        then(keywordRepository).should(times(1)).enableAllByUserId(userId);
    }
}