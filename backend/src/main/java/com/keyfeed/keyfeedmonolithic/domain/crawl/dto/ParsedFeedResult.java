package com.keyfeed.keyfeedmonolithic.domain.crawl.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ParsedFeedResult {
    private final String logoUrl;
    private final List<FeedItem> items;
}
