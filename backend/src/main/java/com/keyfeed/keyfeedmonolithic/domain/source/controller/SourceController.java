package com.keyfeed.keyfeedmonolithic.domain.source.controller;

import com.keyfeed.keyfeedmonolithic.domain.source.dto.RecommendedSourceResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.source.dto.SourceRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.source.dto.SourceResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.source.service.SourceService;
import com.keyfeed.keyfeedmonolithic.global.message.SuccessMessage;
import com.keyfeed.keyfeedmonolithic.global.response.HttpResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sources")
@RequiredArgsConstructor
public class SourceController {

    private final SourceService sourceService;

    @GetMapping("/my")
    public ResponseEntity<?> getMySources(@AuthenticationPrincipal Long userId) {
        List<SourceResponseDto> sources = sourceService.getSourcesByUser(userId);
        return ResponseEntity.ok()
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.READ_SUCCESS.getMessage(), sources));
    }

    @GetMapping("/my/search")
    public ResponseEntity<?> searchMySources(@AuthenticationPrincipal Long userId,
                                             @RequestParam String keyword) {
        List<SourceResponseDto> sources = sourceService.searchMySources(userId, keyword);
        return ResponseEntity.ok()
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.READ_SUCCESS.getMessage(), sources));
    }

    @PostMapping
    public ResponseEntity<?> addSource(@AuthenticationPrincipal Long userId,
                                       @Valid @RequestBody SourceRequestDto request) {
        SourceResponseDto source = sourceService.addSource(userId, request);
        return ResponseEntity.ok()
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.WRITE_SUCCESS.getMessage(), source));
    }

    @DeleteMapping("/my/{userSourceId}")
    public ResponseEntity<?> removeSource(@AuthenticationPrincipal Long userId,
                                          @PathVariable Long userSourceId) {
        sourceService.removeUserSource(userId, userSourceId);
        return ResponseEntity.ok()
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.DELETE_SUCCESS.getMessage(), null));
    }

    @PatchMapping("/my/{userSourceId}/receive-feed")
    public ResponseEntity<?> toggleReceiveFeed(@AuthenticationPrincipal Long userId,
                                               @PathVariable Long userSourceId) {
        SourceResponseDto source = sourceService.toggleReceiveFeed(userId, userSourceId);
        return ResponseEntity.ok()
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.UPDATE_SUCCESS.getMessage(), source));
    }

    @GetMapping("/recommended")
    public ResponseEntity<?> getRecommendedSources(@AuthenticationPrincipal Long userId,
                                                   @PageableDefault(size = 10) Pageable pageable) {
        List<RecommendedSourceResponseDto> recommended = sourceService.getRecommendedSources(userId, pageable);
        return ResponseEntity.ok()
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.READ_SUCCESS.getMessage(), recommended));
    }

}
