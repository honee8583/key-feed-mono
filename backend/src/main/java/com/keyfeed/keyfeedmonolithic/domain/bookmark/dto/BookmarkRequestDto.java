package com.keyfeed.keyfeedmonolithic.domain.bookmark.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkRequestDto {
    private String contentId;
    private Long folderId;
}
