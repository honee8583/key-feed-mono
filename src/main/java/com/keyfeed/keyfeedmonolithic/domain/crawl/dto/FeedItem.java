package com.keyfeed.keyfeedmonolithic.domain.crawl.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FeedItem {
    private String guid;
    private String title;
    private String link;
    private String summary;
    private String thumbnailUrl;
    private LocalDateTime pubDate;
}
