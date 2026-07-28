package com.keyfeed.keyfeedmonolithic.domain.search.controller;

import com.keyfeed.keyfeedmonolithic.domain.search.dto.TrendingSearchResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.search.service.TrendingSearchService;
import com.keyfeed.keyfeedmonolithic.global.message.SuccessMessage;
import com.keyfeed.keyfeedmonolithic.global.response.HttpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class TrendingSearchController {

    private final TrendingSearchService trendingSearchService;

    @GetMapping("/trending")
    public ResponseEntity<?> getTrendingSearches() {
        List<TrendingSearchResponseDto> trending = trendingSearchService.getTrendingSearches();
        return ResponseEntity.ok()
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.READ_SUCCESS.getMessage(), trending));
    }

}
