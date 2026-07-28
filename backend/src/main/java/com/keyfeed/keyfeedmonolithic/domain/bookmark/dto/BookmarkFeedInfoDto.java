package com.keyfeed.keyfeedmonolithic.domain.bookmark.dto;

import com.keyfeed.keyfeedmonolithic.domain.bookmark.entity.Bookmark;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.entity.BookmarkFolder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

@Getter
@Builder
@AllArgsConstructor
public class BookmarkFeedInfoDto {

    private final Long bookmarkId;
    private final Long folderId;
    private final String folderName;

    public static BookmarkFeedInfoDto from(Bookmark bookmark) {
        BookmarkFolder folder = bookmark.getBookmarkFolder();
        return BookmarkFeedInfoDto.builder()
                .bookmarkId(bookmark.getId())
                .folderId(Optional.ofNullable(folder).map(BookmarkFolder::getId).orElse(null))
                .folderName(Optional.ofNullable(folder).map(BookmarkFolder::getName).orElse(null))
                .build();
    }
}
