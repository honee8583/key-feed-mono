package com.keyfeed.keyfeedmonolithic.domain.feed.service;

import com.keyfeed.keyfeedmonolithic.domain.content.dto.ContentFeedResponseDto;
import com.keyfeed.keyfeedmonolithic.global.response.CommonPageResponse;

import java.util.List;
import java.util.Map;

public interface FeedService {

    Map<Long, String> fetchUserSourceMapping(Long userId);

    CommonPageResponse<ContentFeedResponseDto> getPersonalizedFeedsFromMySQL(Long userId, Map<Long, String> sourceMapping, Long lastId, int size, String keyword);

}
