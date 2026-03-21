package com.keyfeed.keyfeedmonolithic.domain.bookmark.service;

import com.keyfeed.keyfeedmonolithic.domain.bookmark.dto.BookmarkFolderRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.dto.BookmarkFolderResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.dto.BookmarkRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.dto.BookmarkResponseDto;
import com.keyfeed.keyfeedmonolithic.global.response.CursorPage;

import java.util.List;
import java.util.Map;

public interface BookmarkService {

    Long createFolder(Long userId, BookmarkFolderRequestDto request);

    void updateFolder(Long userId, Long folderId, BookmarkFolderRequestDto request);

    void deleteFolder(Long userId, Long folderId);

    Long addBookmark(Long userId, BookmarkRequestDto request);

    CursorPage<BookmarkResponseDto> getBookmarks(Long userId, Long lastId, Long folderId, int size);

    void deleteBookmark(Long userId, Long bookmarkId);

    void removeBookmarkFromFolder(Long userId, Long bookmarkId);

    void moveBookmark(Long userId, Long bookmarkId, Long folderId);

    List<BookmarkFolderResponseDto> getFolders(Long userId);

    Map<String, Long> getBookmarkMap(Long userId, List<String> contentIds);

}
