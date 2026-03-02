package com.keyfeed.keyfeedmonolithic.domain.feed.controller;

import com.keyfeed.keyfeedmonolithic.domain.content.dto.ContentFeedResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.feed.service.FeedService;
import com.keyfeed.keyfeedmonolithic.global.response.CommonPageResponse;
import com.keyfeed.keyfeedmonolithic.global.response.HttpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.keyfeed.keyfeedmonolithic.global.message.SuccessMessage.READ_SUCCESS;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping
    public ResponseEntity<?> getMyFeeds(@AuthenticationPrincipal Long userId,
                                        @RequestParam(value = "lastId", required = false) Long lastId,
                                        @RequestParam(value = "size", defaultValue = "10") int size) {
        Map<Long, String> sourceMapping = feedService.fetchUserSourceMapping(userId);
        CommonPageResponse<ContentFeedResponseDto> feeds = feedService.getPersonalizedFeeds(userId, sourceMapping, lastId, size);
        return ResponseEntity.ok()
                .body(new HttpResponse(HttpStatus.OK, READ_SUCCESS.getMessage(), feeds));
    }

}
