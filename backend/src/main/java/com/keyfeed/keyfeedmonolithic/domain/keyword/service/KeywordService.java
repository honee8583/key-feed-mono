package com.keyfeed.keyfeedmonolithic.domain.keyword.service;

import com.keyfeed.keyfeedmonolithic.domain.keyword.dto.KeywordResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.keyword.dto.TrendingKeywordResponseDto;

import java.util.List;
import java.util.Set;

public interface KeywordService {

    List<KeywordResponseDto> getKeywords(Long userId);

    KeywordResponseDto addKeyword(Long userId, String name);

    KeywordResponseDto toggleKeywordNotification(Long userId, Long keywordId);

    void deleteKeyword(Long userId, Long keywordId);

    List<Long> findUserIdsByKeywordsAndSource(Set<String> keywords, Long sourceId);

    List<TrendingKeywordResponseDto> getTrendingKeywords(int size);

    void deactivateExcessKeywords(Long userId, int keepCount);

    void reactivateAllKeywords(Long userId);

}
