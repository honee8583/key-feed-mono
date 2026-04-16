package com.keyfeed.keyfeedmonolithic.domain.bookmark.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.UserRepository;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.dto.BookmarkFolderRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.dto.BookmarkFolderResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.dto.BookmarkRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.dto.BookmarkResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.entity.Bookmark;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.entity.BookmarkFolder;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.exception.FolderAccessDeniedException;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.exception.FolderLimitExceededException;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.repository.BookmarkFolderRepository;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.repository.BookmarkRepository;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.service.BookmarkService;
import com.keyfeed.keyfeedmonolithic.domain.content.dto.ContentFeedResponseDto;
import com.keyfeed.keyfeedmonolithic.domain.content.repository.ContentRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.SubscriptionStatus;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.SubscriptionRepository;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityAlreadyExistsException;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityNotFoundException;
import com.keyfeed.keyfeedmonolithic.global.response.CursorPage;
import com.keyfeed.keyfeedmonolithic.global.util.CursorPagination;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final BookmarkFolderRepository folderRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Value("${app.limits.folder-max-count}")
    private int folderMaxCount;

    @Value("${app.limits.folder-subscriber-max-count}")
    private int folderSubscriberMaxCount;

    /**
     * 폴더 생성
     */
    @Override
    @Transactional
    public Long createFolder(Long userId, BookmarkFolderRequestDto request) {
        User user = getUser(userId);

        validateFolderNameNotDuplicated(userId, request.getName());
        validateFolderCountLimit(userId);

        BookmarkFolder folder = BookmarkFolder.builder()
                .user(user)
                .name(request.getName())
                .icon(request.getIcon())
                .color(request.getColor())
                .build();
        folderRepository.save(folder);

        return folder.getId();
    }

    @Override
    @Transactional
    public void updateFolder(Long userId, Long folderId, BookmarkFolderRequestDto request) {
        BookmarkFolder folder = resolveFolder(folderId);

        if (!folder.getUser().getId().equals(userId)) {
            throw new FolderAccessDeniedException();
        }

        if (!folder.getName().equals(request.getName())) {
            validateFolderNameNotDuplicated(userId, request.getName());
        }

        folder.update(request.getName(), request.getIcon(), request.getColor());
    }

    @Override
    @Transactional
    public void deleteFolder(Long userId, Long folderId) {
        BookmarkFolder folder = getBookmarkFolder(folderId);

        validateFolderOwner(userId, folder);

        bookmarkRepository.updateFolderToNull(folderId);

        folderRepository.delete(folder);
    }

    /**
     * 북마크 추가
     */
    @Override
    @Transactional
    public Long addBookmark(Long userId, BookmarkRequestDto request) {
        User user = getUser(userId);

        validateBookmarkNotDuplicated(userId, request.getContentId());

        BookmarkFolder folder = resolveFolder(request.getFolderId());

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .contentId(request.getContentId())
                .bookmarkFolder(folder)
                .build();
        bookmarkRepository.save(bookmark);

        return bookmark.getId();
    }

    /**
     * 북마크 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public CursorPage<BookmarkResponseDto> getBookmarks(Long userId,
                                                        Long lastId,
                                                        Long folderId,
                                                        int size) {
        Pageable pageable = PageRequest.of(0, size + 1);  // size + 1 만큼 조회 (다음 페이지 존재 여부 판단용)
        List<Bookmark> bookmarks = findBookmarksByFolderCondition(userId, lastId, folderId, pageable);

        if (bookmarks.isEmpty()) {
            return CursorPagination.paginate(Collections.emptyList(), size, BookmarkResponseDto::getBookmarkId);
        }

        Map<String, ContentFeedResponseDto> contentMap = fetchContentMap(bookmarks);

        List<BookmarkResponseDto> responseContent = bookmarks.stream()
                .map(bookmark -> BookmarkResponseDto.of(bookmark, contentMap.get(bookmark.getContentId())))
                .toList();

        return CursorPagination.paginate(responseContent, size, BookmarkResponseDto::getBookmarkId);
    }

    /**
     * 북마크 삭제
     */
    @Override
    @Transactional
    public void deleteBookmark(Long userId, Long bookmarkId) {
        Bookmark bookmark = getBookmarkOwnedByUser(userId, bookmarkId);
        bookmarkRepository.delete(bookmark);
    }

    /**
     * 북마크를 폴더에서 제거
     */
    @Override
    @Transactional
    public void removeBookmarkFromFolder(Long userId, Long bookmarkId) {
        Bookmark bookmark = getBookmarkOwnedByUser(userId, bookmarkId);
        bookmark.removeFolder();
    }

    /**
     * 북마크의 폴더 이동
     */
    @Override
    @Transactional
    public void moveBookmark(Long userId, Long bookmarkId, Long folderId) {
        Bookmark bookmark = getBookmarkOwnedByUser(userId, bookmarkId);
        BookmarkFolder folder = resolveFolderForMove(userId, folderId);
        bookmark.changeFolder(folder);
    }

    @Override
    public List<BookmarkFolderResponseDto> getFolders(Long userId) {
        return folderRepository.findAllByUserIdOrderById(userId).stream()
                .map(BookmarkFolderResponseDto::from)
                .toList();
    }

    @Override
    public Map<String, Long> getBookmarkMap(Long userId, List<String> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return bookmarkRepository.findAllByUserIdAndContentIdIn(userId, contentIds)
                .stream()
                .collect(Collectors.toMap(Bookmark::getContentId, Bookmark::getId));
    }

    private Map<String, ContentFeedResponseDto> fetchContentMap(List<Bookmark> bookmarks) {
        List<Long> contentIds = bookmarks.stream()
                .map(Bookmark::getContentId)
                .map(Long::parseLong)
                .toList();
        return contentRepository.findAllById(contentIds).stream()
                .collect(Collectors.toMap(
                        content -> String.valueOf(content.getId()),
                        content -> ContentFeedResponseDto.from(content, null)
                ));
    }

    // 북마크 폴더 이름이 중복되는지 검증
    private void validateFolderNameNotDuplicated(Long userId, String folderName) {
        if (folderRepository.existsByUserIdAndName(userId, folderName)) {
            throw new EntityAlreadyExistsException("BookmarkFolder", "name: " + folderName);
        }
    }

    // 북마크 폴더 최대 개수를 넘지 않는지 검증
    private void validateFolderCountLimit(Long userId) {
        int limit;
        ErrorMessage errorMessage;
        if (hasFolderBenefit(userId)) {
            limit = folderSubscriberMaxCount;
            errorMessage = ErrorMessage.BOOKMARK_FOLDER_SUBSCRIBER_LIMIT_EXCEEDED;
        } else {
            limit = folderMaxCount;
            errorMessage = ErrorMessage.BOOKMARK_FOLDER_LIMIT_EXCEEDED;
        }

        if (folderRepository.countByUserId(userId) >= limit) {
            throw new FolderLimitExceededException(errorMessage);
        }
    }

    // 구독 혜택(폴더 한도 확대) 보유 여부 확인
    private boolean hasFolderBenefit(Long userId) {
        return subscriptionRepository.existsByUserIdAndStatusIn(
                userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED));
    }

    // 북마크가 이미 존재하는지 검증
    private void validateBookmarkNotDuplicated(Long userId, String contentId) {
        if (bookmarkRepository.existsByUserIdAndContentId(userId, contentId)) {
            throw new EntityAlreadyExistsException("Bookmark", contentId);
        }
    }

    // 폴더 조회
    private BookmarkFolder resolveFolder(Long folderId) {
        if (folderId == null) {
            return null;
        }
        return getBookmarkFolder(folderId);
    }

    // 이동할 폴더 조회 및 검증
    private BookmarkFolder resolveFolderForMove(Long userId, Long folderId) {
        if (folderId == null || folderId == 0L) {
            return null;
        }

        BookmarkFolder folder = getBookmarkFolder(folderId);
        validateFolderOwner(userId, folder);
        return folder;
    }

    // 북마크 폴더의 주인인지 확인
    private void validateFolderOwner(Long userId, BookmarkFolder folder) {
        if (!folder.getUser().getId().equals(userId)) {
            throw new FolderAccessDeniedException();
        }
    }

    private List<Bookmark> findBookmarksByFolderCondition(Long userId,
                                                          Long lastId,
                                                          Long folderId,
                                                          Pageable pageable) {
        if (folderId == null) {
            return findAllBookmarks(userId, lastId, pageable);
        }
        if (folderId == 0) {
            return findUncategorizedBookmarks(userId, lastId, pageable);
        }
        return findBookmarksInFolder(userId, lastId, folderId, pageable);
    }

    // 모든 북마크 조회 (커서 페이징)
    private List<Bookmark> findAllBookmarks(Long userId, Long lastId, Pageable pageable) {
        if (lastId == null) {
            return bookmarkRepository.findAllByUserIdOrderByIdDesc(userId, pageable);
        }
        return bookmarkRepository.findAllByUserIdAndIdLessThanOrderByIdDesc(
                userId, lastId, pageable
        );
    }

    // 폴더가 없는 북마크 조회
    private List<Bookmark> findUncategorizedBookmarks(Long userId, Long lastId, Pageable pageable) {
        if (lastId == null) {
            return bookmarkRepository.findAllByUserIdAndBookmarkFolderIsNullOrderByIdDesc(userId, pageable);
        }
        return bookmarkRepository
                .findAllByUserIdAndBookmarkFolderIsNullAndIdLessThanOrderByIdDesc(userId, lastId, pageable);
    }

    // 특정 폴더의 북마크 목록 조회
    private List<Bookmark> findBookmarksInFolder(Long userId,
                                                 Long lastId,
                                                 Long folderId,
                                                 Pageable pageable) {
        if (lastId == null) {
            return bookmarkRepository.findAllByUserIdAndBookmarkFolderIdOrderByIdDesc(userId, folderId, pageable);
        }
        return bookmarkRepository.findAllByUserIdAndBookmarkFolderIdAndIdLessThanOrderByIdDesc(userId, folderId, lastId, pageable);
    }

    private Bookmark getBookmarkOwnedByUser(Long userId, Long bookmarkId) {
        return bookmarkRepository.findByIdAndUserId(bookmarkId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Bookmark", bookmarkId));
    }

    private BookmarkFolder getBookmarkFolder(Long folderId) {
        return folderRepository.findById(folderId).orElseThrow(() -> new EntityNotFoundException("BookmarkFolder", folderId));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User", userId));
    }

}
