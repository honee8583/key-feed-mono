package com.keyfeed.keyfeedmonolithic.domain.keyword.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.UserRepository;
import com.keyfeed.keyfeedmonolithic.domain.keyword.dto.KeywordResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.keyword.dto.TrendingKeywordResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.keyword.entity.Keyword;
import com.keyfeed.keyfeedmonolithic.domain.keyword.exception.KeywordLimitExceededException;
import com.keyfeed.keyfeedmonolithic.domain.keyword.repository.KeywordRepository;
import com.keyfeed.keyfeedmonolithic.domain.keyword.service.KeywordService;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.SubscriptionStatus;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.SubscriptionRepository;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityAlreadyExistsException;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityNotFoundException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class KeywordServiceImpl implements KeywordService {

    private final KeywordRepository keywordRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Value("${app.limits.keyword-max-count}")
    private int keywordMaxCount;

    @Value("${app.limits.keyword-subscriber-max-count}")
    private int keywordSubscriberMaxCount;

    @Override
    @Transactional(readOnly = true)
    public List<KeywordResponseDto> getKeywords(Long userId) {
        List<Keyword> keywords = keywordRepository.findByUserId(userId);
        return keywords.stream()
                .map(KeywordResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public KeywordResponseDto addKeyword(Long userId, String name) {
        User user = findUserById(userId);

        if (keywordRepository.existsByNameAndUser(name, user)) {
            throw new EntityAlreadyExistsException("Keyword", name);
        }

        int limit;
        ErrorMessage errorMessage;
        if (hasKeywordBenefit(userId)) {
            limit = keywordSubscriberMaxCount;
            errorMessage = ErrorMessage.KEYWORD_SUBSCRIBER_LIMIT_EXCEEDED;
        } else {
            limit = keywordMaxCount;
            errorMessage = ErrorMessage.KEYWORD_LIMIT_EXCEEDED;
        }

        if (keywordRepository.countByUserIdAndIsEnabledTrue(userId) >= limit) {
            throw new KeywordLimitExceededException(errorMessage);
        }

        Keyword keyword = Keyword.builder()
                .user(user)
                .name(name)
                .build();
        keywordRepository.save(keyword);

        return KeywordResponseDto.from(keyword);
    }

    @Override
    @Transactional
    public KeywordResponseDto toggleKeywordNotification(Long userId, Long keywordId) {
        Keyword keyword = findKeywordByIdAndUserId(keywordId, userId);
        keyword.setNotificationEnabled(!keyword.isNotificationEnabled());
        keywordRepository.save(keyword);

        return KeywordResponseDto.from(keyword);
    }

    @Override
    @Transactional
    public void deleteKeyword(Long userId, Long keywordId) {
        Keyword keyword = findKeywordByIdAndUserId(keywordId, userId);
        keywordRepository.delete(keyword);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findUserIdsByKeywordsAndSource(Set<String> keywords, Long sourceId) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }

        return keywordRepository.findUserIdsByNamesAndSourceId(keywords, sourceId);
    }

    private static final int TRENDING_MAX_SIZE = 10;

    @Override
    @Transactional(readOnly = true)
    public List<TrendingKeywordResponseDto> getTrendingKeywords(int size) {
        int limitedSize = Math.min(Math.max(size, 1), TRENDING_MAX_SIZE);
        return keywordRepository.findTrendingKeywords(PageRequest.of(0, limitedSize))
                .stream()
                .map(projection -> new TrendingKeywordResponseDto(projection.getName(), projection.getUserCount()))
                .toList();
    }

    @Override
    @Transactional
    public void deactivateExcessKeywords(Long userId, int keepCount) {
        List<Keyword> keywords = keywordRepository.findByUserIdOrderByCreatedAtAsc(userId);
        for (int i = keepCount; i < keywords.size(); i++) {
            keywords.get(i).disable();
        }
    }

    @Override
    @Transactional
    public void reactivateAllKeywords(Long userId) {
        keywordRepository.enableAllByUserId(userId);
    }

    private boolean hasKeywordBenefit(Long userId) {
        return subscriptionRepository.existsByUserIdAndStatusIn(
                userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));
    }

    private Keyword findKeywordByIdAndUserId(Long keywordId, Long userId) {
        return keywordRepository.findByIdAndUserId(keywordId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Keyword", keywordId));
    }

}