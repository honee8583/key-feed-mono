package com.keyfeed.keyfeedmonolithic.domain.bookmark.repository;

import com.keyfeed.keyfeedmonolithic.domain.bookmark.entity.Bookmark;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // 전체 북마크 첫 페이지
    @Query("SELECT b FROM Bookmark b " +
            "LEFT JOIN FETCH b.bookmarkFolder " +
            "WHERE b.user.id = :userId " +
            "ORDER BY b.id DESC")
    List<Bookmark> findAllByUserIdOrderByIdDesc(@Param("userId") Long userId, Pageable pageable);

    // 전체 북마크 다음 페이지
    @Query("SELECT b FROM Bookmark b " +
            "LEFT JOIN FETCH b.bookmarkFolder " +
            "WHERE b.user.id = :userId AND b.id < :lastId " +
            "ORDER BY b.id DESC")
    List<Bookmark> findAllByUserIdAndIdLessThanOrderByIdDesc(@Param("userId") Long userId,
                                                             @Param("lastId") Long lastId,
                                                             Pageable pageable);

    // 특정 폴더 첫 페이지
    @Query("SELECT b FROM Bookmark b " +
            "LEFT JOIN FETCH b.bookmarkFolder " +
            "WHERE b.user.id = :userId AND b.bookmarkFolder.id = :folderId " +
            "ORDER BY b.id DESC")
    List<Bookmark> findAllByUserIdAndBookmarkFolderIdOrderByIdDesc(@Param("userId") Long userId,
                                                                   @Param("folderId") Long folderId,
                                                                   Pageable pageable);

    // 특정 폴더 다음 페이지
    @Query("SELECT b FROM Bookmark b " +
            "LEFT JOIN FETCH b.bookmarkFolder " +
            "WHERE b.user.id = :userId AND b.bookmarkFolder.id = :folderId AND b.id < :lastId " +
            "ORDER BY b.id DESC")
    List<Bookmark> findAllByUserIdAndBookmarkFolderIdAndIdLessThanOrderByIdDesc(@Param("userId") Long userId,
                                                                                @Param("folderId") Long folderId,
                                                                                @Param("lastId") Long lastId,
                                                                                Pageable pageable);

    // 미분류 폴더 첫 페이지
    List<Bookmark> findAllByUserIdAndBookmarkFolderIsNullOrderByIdDesc(Long userId, Pageable pageable);

    // 미분류 폴더 다음 페이지
    List<Bookmark> findAllByUserIdAndBookmarkFolderIsNullAndIdLessThanOrderByIdDesc(Long userId, Long id, Pageable pageable);

    // 중복 체크용
    boolean existsByUserIdAndContentId(Long userId, String contentId);

    // 본인의 북마크인지 확인하며 단건 조회
    Optional<Bookmark> findByIdAndUserId(Long id, Long userId);

    // contentId에 해당하는 북마크 목록 조회
    List<Bookmark> findAllByUserIdAndContentIdIn(Long userId, List<String> contentIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Bookmark b SET b.bookmarkFolder = null WHERE b.bookmarkFolder.id = :folderId")
    void updateFolderToNull(@Param("folderId") Long folderId);

    @Modifying
    @Query("DELETE FROM Bookmark b WHERE b.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

}
