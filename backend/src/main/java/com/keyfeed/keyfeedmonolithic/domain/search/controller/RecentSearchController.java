package com.keyfeed.keyfeedmonolithic.domain.search.controller;

import com.keyfeed.keyfeedmonolithic.domain.search.service.RecentSearchService;
import com.keyfeed.keyfeedmonolithic.global.message.SuccessMessage;
import com.keyfeed.keyfeedmonolithic.global.response.HttpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class RecentSearchController {

    private final RecentSearchService recentSearchService;

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentSearches(@AuthenticationPrincipal Long userId) {
        List<String> recentSearches = recentSearchService.getRecentSearches(userId);
        return ResponseEntity.ok()
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.READ_SUCCESS.getMessage(), recentSearches));
    }

    @DeleteMapping("/recent/{keyword}")
    public ResponseEntity<?> deleteRecentSearch(@AuthenticationPrincipal Long userId,
                                                @PathVariable("keyword") String keyword) {
        recentSearchService.delete(userId, keyword);
        return ResponseEntity.ok()
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.DELETE_SUCCESS.getMessage(), null));
    }

    @DeleteMapping("/recent")
    public ResponseEntity<?> deleteAllRecentSearches(@AuthenticationPrincipal Long userId) {
        recentSearchService.deleteAll(userId);
        return ResponseEntity.ok()
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.DELETE_SUCCESS.getMessage(), null));
    }

}
