package com.keyfeed.keyfeedmonolithic.domain.bookmark.dto;

import com.keyfeed.keyfeedmonolithic.domain.bookmark.entity.Bookmark;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.entity.BookmarkFolder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

/**
 * 피드 응답에 북마크 상태를 붙이기 위한 경량 DTO.
 * 북마크 ID와 함께 현재 속한 폴더 정보(미분류면 null)를 담는다.
 */
@Getter
@Builder
@AllArgsConstructor
public class BookmarkFeedInfoDto {

    private final Long bookmarkId;
    private final Long folderId;   // 미분류면 null
    private final String folderName; // 미분류면 null

    public static BookmarkFeedInfoDto from(Bookmark bookmark) {
        BookmarkFolder folder = bookmark.getBookmarkFolder();
        return BookmarkFeedInfoDto.builder()
                .bookmarkId(bookmark.getId())
                .folderId(Optional.ofNullable(folder).map(BookmarkFolder::getId).orElse(null))
                .folderName(Optional.ofNullable(folder).map(BookmarkFolder::getName).orElse(null))
                .build();
    }
}
