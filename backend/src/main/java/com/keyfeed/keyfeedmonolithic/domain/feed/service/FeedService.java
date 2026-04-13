package com.keyfeed.keyfeedmonolithic.domain.feed.service;

import com.keyfeed.keyfeedmonolithic.domain.content.dto.ContentFeedResponseDto;
import com.keyfeed.keyfeedmonolithic.global.response.CommonPageResponse;
import com.keyfeed.keyfeedmonolithic.global.response.OffsetPageResponse;

public interface FeedService {

    CommonPageResponse<ContentFeedResponseDto> getPersonalizedFeeds(Long userId, Long lastId, int size, String keyword);

    OffsetPageResponse<ContentFeedResponseDto> getPersonalizedFeedsWithOffset(Long userId, int page, int size, String keyword);

}
