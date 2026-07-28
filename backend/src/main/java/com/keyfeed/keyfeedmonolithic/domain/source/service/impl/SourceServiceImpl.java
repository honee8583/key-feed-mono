package com.keyfeed.keyfeedmonolithic.domain.source.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.keyword.entity.Keyword;
import com.keyfeed.keyfeedmonolithic.domain.keyword.repository.KeywordRepository;
import com.keyfeed.keyfeedmonolithic.domain.source.dto.RecommendedSourceResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.source.dto.SourceResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.source.entity.UserSource;
import com.keyfeed.keyfeedmonolithic.domain.source.repository.SourceRepository;
import com.keyfeed.keyfeedmonolithic.domain.source.repository.UserSourceRepository;
import com.keyfeed.keyfeedmonolithic.domain.source.service.SourceService;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SourceServiceImpl implements SourceService {

    private final SourceRepository sourceRepository;
    private final UserSourceRepository userSourceRepository;
    private final KeywordRepository keywordRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SourceResponseDto> getSourcesByUser(Long userId) {
        List<UserSource> userSources = userSourceRepository.findByUserId(userId);
        return convertToResponseDtos(userSources);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SourceResponseDto> getActiveSourcesByUser(Long userId) {
        List<UserSource> userSources = userSourceRepository.findByUserIdAndReceiveFeedTrue(userId);
        return convertToResponseDtos(userSources);
    }

    @Override
    public void removeUserSource(Long userId, Long userSourceId) {
        UserSource userSource = userSourceRepository.findByIdAndUserId(userSourceId, userId)
                .orElseThrow(() -> new EntityNotFoundException("UserSource", userSourceId));
        userSourceRepository.delete(userSource);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SourceResponseDto> searchMySources(Long userId, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return getSourcesByUser(userId);
        }

        List<UserSource> userSources = userSourceRepository.searchByUserIdAndKeyword(userId, keyword);
        return convertToResponseDtos(userSources);
    }

    @Override
    public SourceResponseDto toggleReceiveFeed(Long userId, Long userSourceId) {
        UserSource userSource = userSourceRepository.findByIdAndUserId(userSourceId, userId)
                .orElseThrow(() -> new EntityNotFoundException("UserSource", userSourceId));
        userSource.toggleReceiveFeed();
        return SourceResponseDto.from(userSource);
    }

    private List<SourceResponseDto> convertToResponseDtos(List<UserSource> userSources) {
        return userSources.stream()
                .map(SourceResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendedSourceResponseDto> getRecommendedSources(Long userId, Pageable pageable) {
        List<String> userKeywords = keywordRepository.findByUserId(userId)
                .stream()
                .map(Keyword::getName)
                .toList();

        if (userKeywords.isEmpty()) {
            return Collections.emptyList();
        }

        return sourceRepository.findRecommendedSourcesByKeywords(userKeywords, userId, pageable)
                .stream()
                .map(source -> RecommendedSourceResponseDto.builder()
                        .sourceId(source.getSourceId())
                        .name(source.getName())
                        .url(source.getUrl())
                        .subscriberCount(source.getSubscriberCount())
                        .build())
                .toList();
    }
}
