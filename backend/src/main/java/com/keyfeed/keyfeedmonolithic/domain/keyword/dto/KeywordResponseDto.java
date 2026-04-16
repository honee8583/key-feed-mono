package com.keyfeed.keyfeedmonolithic.domain.keyword.dto;

import com.keyfeed.keyfeedmonolithic.domain.keyword.entity.Keyword;
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
    private Boolean isEnabled;

    public static KeywordResponseDto from(Keyword keyword) {
        return KeywordResponseDto.builder()
                .keywordId(keyword.getId())
                .name(keyword.getName())
                .isNotificationEnabled(keyword.isNotificationEnabled())
                .isEnabled(keyword.isEnabled())
                .build();
    }
}
