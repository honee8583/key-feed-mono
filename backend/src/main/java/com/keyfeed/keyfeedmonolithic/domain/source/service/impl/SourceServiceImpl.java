package com.keyfeed.keyfeedmonolithic.domain.source.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.UserRepository;
import com.keyfeed.keyfeedmonolithic.domain.keyword.entity.Keyword;
import com.keyfeed.keyfeedmonolithic.domain.keyword.repository.KeywordRepository;
import com.keyfeed.keyfeedmonolithic.domain.source.dto.RecommendedSourceResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.source.dto.SourceRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.source.dto.SourceResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.source.entity.Source;
import com.keyfeed.keyfeedmonolithic.domain.source.entity.UserSource;
import com.keyfeed.keyfeedmonolithic.domain.source.exception.InvalidRssUrlException;
import com.keyfeed.keyfeedmonolithic.domain.source.exception.SourceValidationException;
import com.keyfeed.keyfeedmonolithic.domain.source.repository.SourceRepository;
import com.keyfeed.keyfeedmonolithic.domain.source.repository.UserSourceRepository;
import com.keyfeed.keyfeedmonolithic.domain.source.service.SourceService;
import com.keyfeed.keyfeedmonolithic.domain.source.validator.RobotsTxtValidator;
import com.keyfeed.keyfeedmonolithic.domain.source.validator.RssFeedValidator;
import com.keyfeed.keyfeedmonolithic.domain.source.validator.UrlValidator;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityAlreadyExistsException;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityNotFoundException;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.keyfeed.keyfeedmonolithic.global.constant.HttpConstants.USER_AGENT;
import static com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage.RSS_PARSING_FAILED;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SourceServiceImpl implements SourceService {

    private final SourceRepository sourceRepository;
    private final UserSourceRepository userSourceRepository;
    private final UserRepository userRepository;
    private final KeywordRepository keywordRepository;
    private final UrlValidator urlValidator;
    private final RobotsTxtValidator robotsTxtValidator;
    private final RssFeedValidator rssFeedValidator;

    // Jsoup 설정 상수
    private static final int JSOUP_TIMEOUT = 10000; // 10초

    // RSS/Atom 피드 탐지용 CSS 선택자
    private static final String RSS_LINK_SELECTOR = "link[type=application/rss+xml]";
    private static final String ATOM_LINK_SELECTOR = "link[type=application/atom+xml]";

    // HTML 속성
    private static final String HREF_ATTRIBUTE = "abs:href";

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
    public SourceResponseDto addSource(Long userId, SourceRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        log.info("소스 등록 요청 - 사용자: {}, URL: {}", userId, request.getUrl());

        // 1. 기본 URL 검증
        UrlValidator.ValidationResult validationResult = urlValidator.validate(request.getUrl());
        if (!validationResult.isValid()) {
            log.error("URL 검증 실패: {} - {}", request.getUrl(), validationResult.getMessage());
            throw new SourceValidationException(validationResult.getMessage());
        }

        // 2. RSS 피드 주소 탐지
        String rssUrl = discoverRssUrl(request.getUrl());
        log.info("RSS URL 탐지 완료: {} -> {}", request.getUrl(), rssUrl);

        // 3. robots.txt 확인
        if (!robotsTxtValidator.isAllowedToCrawl(rssUrl)) {
            log.error("robots.txt에 의해 크롤링 금지됨: {}", rssUrl);
            throw new SourceValidationException(ErrorMessage.ROBOTS_TXT_DISALLOWED.getMessage());
        }

        // 4. RSS 파싱 가능 여부 테스트
        if (!rssFeedValidator.canParseFeed(rssUrl)) {
            log.error("RSS 피드 파싱 실패: {}", rssUrl);
            throw new SourceValidationException(RSS_PARSING_FAILED.getMessage());
        }

        log.info("모든 검증 통과: {}", rssUrl);

        // 5. Source 저장
        // 기존에 같은 source가 존재하지 않는다면 생성
        Source source = sourceRepository.findByUrl(rssUrl)
                .orElseGet(() -> {
                    Source newSource = Source.builder()
                            .url(rssUrl)
                            .build();
                    return sourceRepository.save(newSource);
                });

        // 이미 내가 등록한 소스인지 확인
        if (userSourceRepository.existsByUserIdAndSourceId(userId, source.getId())) {
            throw new EntityAlreadyExistsException("UserSource", "userId: " + userId + ", sourceId: " + source.getId());
        }

        UserSource userSource = UserSource.builder()
                .user(user)
                .source(source)
                .userDefinedName(request.getName())
                .receiveFeed(request.getReceiveFeed() != null ? request.getReceiveFeed() : true)
                .build();
        userSourceRepository.save(userSource);

        log.info("소스 등록 완료 - 사용자: {}, 소스ID: {}, RSS URL: {}", userId, source.getId(), rssUrl);

        return SourceResponseDto.from(userSource);
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

    private String discoverRssUrl(String inputUrl) {
        try {
            Document doc = Jsoup.connect(inputUrl)
                    .timeout(JSOUP_TIMEOUT)
                    .userAgent(USER_AGENT)
                    .get();

            // RSS 2.0 피드 탐지
            Element rssLink = doc.select(RSS_LINK_SELECTOR).first();
            if (rssLink != null) {
                String discoveredUrl = rssLink.attr(HREF_ATTRIBUTE);
                log.info("RSS 링크 발견: {}", discoveredUrl);
                return discoveredUrl;
            }

            // Atom 피드 탐지
            Element atomLink = doc.select(ATOM_LINK_SELECTOR).first();
            if (atomLink != null) {
                String discoveredUrl = atomLink.attr(HREF_ATTRIBUTE);
                log.info("Atom 링크 발견: {}", discoveredUrl);
                return discoveredUrl;
            }

            log.info("RSS/Atom 링크를 찾지 못함. 원본 URL 사용: {}", inputUrl);
            return inputUrl;

        } catch (IOException e) {
            log.warn("URL 접속 실패: {}. 원인: {}", inputUrl, e.getMessage());
            throw new InvalidRssUrlException();
        } catch (Exception e) {
            log.warn("RSS URL 자동 탐색 실패: {}. 원본 입력을 사용합니다. 오류: {}", inputUrl, e.toString());
            return inputUrl;
        }
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
                        .url(source.getUrl())
                        .subscriberCount(source.getSubscriberCount())
                        .build())
                .toList();
    }
}