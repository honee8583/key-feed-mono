package com.keyfeed.keyfeedmonolithic.domain.bookmark.repository;

import com.keyfeed.keyfeedmonolithic.domain.bookmark.entity.BookmarkFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookmarkFolderRepository extends JpaRepository<BookmarkFolder, Long> {

    // 내 북마크 폴더 목록
    List<BookmarkFolder> findAllByUserIdOrderById(Long userId);

    // 폴더명 중복 검사
    boolean existsByUserIdAndName(Long userId, String name);

    // 사용자의 북마크 폴더 개수
    long countByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM BookmarkFolder bf WHERE bf.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

}
