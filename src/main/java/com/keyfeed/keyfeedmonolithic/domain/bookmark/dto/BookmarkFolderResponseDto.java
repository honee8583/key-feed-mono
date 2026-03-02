package com.keyfeed.keyfeedmonolithic.domain.bookmark.dto;

import com.keyfeed.keyfeedmonolithic.domain.bookmark.entity.BookmarkFolder;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookmarkFolderResponseDto {
    private Long folderId;
    private String name;
    private String icon;
    private String color;

    public static BookmarkFolderResponseDto from(BookmarkFolder folder) {
        return BookmarkFolderResponseDto.builder()
                .folderId(folder.getId())
                .name(folder.getName())
                .icon(folder.getIcon())
                .color(folder.getColor())
                .build();
    }
}