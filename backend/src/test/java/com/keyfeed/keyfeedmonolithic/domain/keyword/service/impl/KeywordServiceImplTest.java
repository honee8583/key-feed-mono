package com.keyfeed.keyfeedmonolithic.domain.keyword.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.UserRepository;
import com.keyfeed.keyfeedmonolithic.domain.keyword.dto.KeywordResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.keyword.entity.Keyword;
import com.keyfeed.keyfeedmonolithic.domain.keyword.exception.KeywordLimitExceededException;
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

    private static final int KEYWORD_MAX_COUNT = 3;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keywordService, "keywordMaxCount", KEYWORD_MAX_COUNT);
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
        given(keywordRepository.countByUserId(userId)).willReturn(2L);
        given(keywordRepository.save(any(Keyword.class))).willReturn(keyword);

        // when
        KeywordResponseDto result = keywordService.addKeyword(userId, keywordName);

        // then
        assertThat(result.getName()).isEqualTo(keywordName);
        then(keywordRepository).should(times(1)).save(any(Keyword.class));
    }

    @Test
    @DisplayName("비구독자 - 한도(3개) 초과 시 KeywordLimitExceededException 발생")
    void 비구독자_한도_초과_예외_발생() {
        // given
        Long userId = 2L;
        String keywordName = "자바";
        User user = makeUser(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(false);
        given(keywordRepository.countByUserId(userId)).willReturn(3L);

        // when & then
        assertThatThrownBy(() -> keywordService.addKeyword(userId, keywordName))
                .isInstanceOf(KeywordLimitExceededException.class)
                .hasMessageContaining("구독 시 무제한으로 이용하실 수 있습니다");

        then(keywordRepository).should(never()).save(any(Keyword.class));
    }

    @Test
    @DisplayName("구독자(ACTIVE) - 한도 초과해도 키워드 추가 성공")
    void 구독자_한도_초과해도_키워드_추가_성공() {
        // given
        Long userId = 3L;
        String keywordName = "코틀린";
        User user = makeUser(userId);
        Keyword keyword = makeKeyword(1L, user, keywordName);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(true);
        given(keywordRepository.save(any(Keyword.class))).willReturn(keyword);

        // when
        KeywordResponseDto result = keywordService.addKeyword(userId, keywordName);

        // then
        assertThat(result.getName()).isEqualTo(keywordName);
        then(keywordRepository).should(never()).countByUserId(userId);
        then(keywordRepository).should(times(1)).save(any(Keyword.class));
    }

    @Test
    @DisplayName("구독자(ACTIVE) - 한도(100개)를 크게 초과해도 키워드 추가 성공")
    void 구독자_대량_키워드_추가_성공() {
        // given
        Long userId = 4L;
        String keywordName = "리액트";
        User user = makeUser(userId);
        Keyword keyword = makeKeyword(1L, user, keywordName);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(true);
        given(keywordRepository.save(any(Keyword.class))).willReturn(keyword);

        // when
        KeywordResponseDto result = keywordService.addKeyword(userId, keywordName);

        // then
        assertThat(result).isNotNull();
        then(keywordRepository).should(never()).countByUserId(any());
    }

    @Test
    @DisplayName("구독자(CANCELED, 만료 전) - 한도 초과해도 키워드 추가 성공")
    void 구독_취소_만료전_한도_초과해도_키워드_추가_성공() {
        // given
        Long userId = 8L;
        String keywordName = "쿠버네티스";
        User user = makeUser(userId);
        Keyword keyword = makeKeyword(1L, user, keywordName);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(true);
        given(keywordRepository.save(any(Keyword.class))).willReturn(keyword);

        // when
        KeywordResponseDto result = keywordService.addKeyword(userId, keywordName);

        // then
        assertThat(result.getName()).isEqualTo(keywordName);
        then(keywordRepository).should(never()).countByUserId(userId);
        then(keywordRepository).should(times(1)).save(any(Keyword.class));
    }

    @Test
    @DisplayName("중복 키워드 등록 시 EntityAlreadyExistsException 발생")
    void 중복_키워드_등록_예외_발생() {
        // given
        Long userId = 5L;
        String keywordName = "중복키워드";
        User user = makeUser(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> keywordService.addKeyword(userId, keywordName))
                .isInstanceOf(EntityAlreadyExistsException.class);

        then(subscriptionRepository).should(never()).existsByUserIdAndStatusIn(any(), any());
        then(keywordRepository).should(never()).save(any(Keyword.class));
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
    }

    @Test
    @DisplayName("비구독자 - 정확히 한도(3개)와 동일한 경우 예외 발생")
    void 비구독자_정확히_한도_동일_예외_발생() {
        // given
        Long userId = 6L;
        String keywordName = "파이썬";
        User user = makeUser(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(false);
        given(keywordRepository.countByUserId(userId)).willReturn((long) KEYWORD_MAX_COUNT);

        // when & then
        assertThatThrownBy(() -> keywordService.addKeyword(userId, keywordName))
                .isInstanceOf(KeywordLimitExceededException.class);
    }

    @Test
    @DisplayName("비구독자 - 한도보다 1개 적을 때 키워드 추가 성공")
    void 비구독자_한도보다_1개_적을때_성공() {
        // given
        Long userId = 7L;
        String keywordName = "도커";
        User user = makeUser(userId);
        Keyword keyword = makeKeyword(1L, user, keywordName);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(keywordRepository.existsByNameAndUser(keywordName, user)).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(false);
        given(keywordRepository.countByUserId(userId)).willReturn((long) KEYWORD_MAX_COUNT - 1);
        given(keywordRepository.save(any(Keyword.class))).willReturn(keyword);

        // when
        KeywordResponseDto result = keywordService.addKeyword(userId, keywordName);

        // then
        assertThat(result.getName()).isEqualTo(keywordName);
    }
}
