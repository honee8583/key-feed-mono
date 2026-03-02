//package com.keyfeed.keyfeedmonolithic.domain.feed.controller;
//
//import com.leedahun.feedservice.domain.feed.dto.ContentFeedResponseDto;
//import com.leedahun.feedservice.domain.feed.service.FeedService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/internal/feeds")
//public class FeedInternalController {
//
//    private final FeedService feedService;
//
//    @PostMapping("/contents")
//    public ResponseEntity<List<ContentFeedResponseDto>> getContentsByIds(@RequestBody List<String> contentIds) {
//        List<ContentFeedResponseDto> contents = feedService.getContentsByIds(contentIds);
//        return ResponseEntity.ok(contents);
//    }
//}