package com.keyfeed.keyfeedmonolithic.domain.bookmark.dto;

import com.keyfeed.keyfeedmonolithic.domain.bookmark.entity.Bookmark;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.entity.BookmarkFolder;
import com.keyfeed.keyfeedmonolithic.domain.content.dto.ContentFeedResponseDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BookmarkResponseDto {
    private Long bookmarkId;
    private Long folderId;
    private String folderName;
    private LocalDateTime createdAt;

    private ContentFeedResponseDto content;

    public static BookmarkResponseDto of(Bookmark bookmark, ContentFeedResponseDto content) {
        BookmarkFolder folder = bookmark.getBookmarkFolder();

        Long folderId = null;
        String folderName = null;

        if (folder != null) {
            folderId = folder.getId();
            folderName = folder.getName();
        }

        return BookmarkResponseDto.builder()
                .bookmarkId(bookmark.getId())
                .folderId(folderId)
                .folderName(folderName)
                .createdAt(bookmark.getCreatedAt())
                .content(content) // 컨텐츠 정보 매핑
                .build();
    }
}
