package com.keyfeed.keyfeedmonolithic.domain.feed.service;

import com.keyfeed.keyfeedmonolithic.domain.content.dto.ContentFeedResponseDto;
import com.keyfeed.keyfeedmonolithic.global.response.CommonPageResponse;

public interface FeedService {

    CommonPageResponse<ContentFeedResponseDto> getPersonalizedFeeds(Long userId, Long lastId, int size, String keyword);

}
