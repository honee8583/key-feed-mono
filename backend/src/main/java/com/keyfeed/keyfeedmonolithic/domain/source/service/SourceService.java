package com.keyfeed.keyfeedmonolithic.domain.source.service;

import com.keyfeed.keyfeedmonolithic.domain.source.dto.RecommendedSourceResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.source.dto.SourceRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.source.dto.SourceResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SourceService {

    List<SourceResponseDto> getSourcesByUser(Long userId);

    List<SourceResponseDto> getActiveSourcesByUser(Long userId);

    SourceResponseDto addSource(Long userId, SourceRequestDto request);

    void removeUserSource(Long userId, Long userSourceId);

    List<SourceResponseDto> searchMySources(Long userId, String keyword);

    SourceResponseDto toggleReceiveFeed(Long userId, Long userSourceId);

    List<RecommendedSourceResponseDto> getRecommendedSources(Long userId, Pageable pageable);

}
