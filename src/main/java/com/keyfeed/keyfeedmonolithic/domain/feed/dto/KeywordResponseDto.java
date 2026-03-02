package com.keyfeed.keyfeedmonolithic.domain.feed.dto;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class KeywordResponseDto {
    private Long keywordId;
    private String name;
    private Boolean isNotificationEnabled;
}
