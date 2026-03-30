package com.keyfeed.keyfeedmonolithic.domain.content.repository;

import com.keyfeed.keyfeedmonolithic.domain.content.entity.Content;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {

    @Query("SELECT c FROM Content c WHERE c.sourceId IN :sourceIds ORDER BY c.id DESC")
    List<Content> findFirstPage(@Param("sourceIds") List<Long> sourceIds, Pageable pageable);

    @Query("SELECT c FROM Content c WHERE c.sourceId IN :sourceIds AND c.id < :lastId ORDER BY c.id DESC")
    List<Content> findNextPage(@Param("sourceIds") List<Long> sourceIds, @Param("lastId") Long lastId, Pageable pageable);

    @Query("SELECT c FROM Content c WHERE c.sourceId IN :sourceIds AND (c.title LIKE %:keyword% OR c.summary LIKE %:keyword%) ORDER BY c.id DESC")
    List<Content> searchFirstPage(@Param("sourceIds") List<Long> sourceIds, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM Content c WHERE c.sourceId IN :sourceIds AND c.id < :lastId AND (c.title LIKE %:keyword% OR c.summary LIKE %:keyword%) ORDER BY c.id DESC")
    List<Content> searchNextPage(@Param("sourceIds") List<Long> sourceIds, @Param("lastId") Long lastId, @Param("keyword") String keyword, Pageable pageable);

    // 구독 소스 + 키워드 기반 content 조회 (첫 페이지)
    @Query(value = """
    SELECT DISTINCT c.*
    FROM content c
    WHERE c.created_at > COALESCE(:lastCheckedAt, '1970-01-01')
    AND (
        c.source_id IN (
            SELECT us.source_id FROM user_source us
            WHERE us.user_id = :userId AND us.receive_feed = 1
        )
        OR EXISTS (
            SELECT 1 FROM keyword k
            WHERE k.user_id = :userId AND k.is_notification_enabled = 1
            AND (c.title LIKE CONCAT('%', k.name, '%') OR c.summary LIKE CONCAT('%', k.name, '%'))
        )
    )
    ORDER BY c.content_id DESC
    LIMIT :size
    """, nativeQuery = true)
    List<Content> findNotificationsFirstPage(@Param("userId") Long userId,
                                             @Param("lastCheckedAt") LocalDateTime lastCheckedAt,
                                             @Param("size") int size);

    // 구독 소스 + 키워드 기반 content 조회 (다음 페이지)
    @Query(value = """
    SELECT DISTINCT c.*
    FROM content c
    WHERE c.created_at > COALESCE(:lastCheckedAt, '1970-01-01')
    AND c.content_id < :lastId
    AND (
        c.source_id IN (
            SELECT us.source_id FROM user_source us
            WHERE us.user_id = :userId AND us.receive_feed = 1
        )
        OR EXISTS (
            SELECT 1 FROM keyword k
            WHERE k.user_id = :userId AND k.is_notification_enabled = 1
            AND (c.title LIKE CONCAT('%', k.name, '%') OR c.summary LIKE CONCAT('%', k.name, '%'))
        )
    )
    ORDER BY c.content_id DESC
    LIMIT :size
    """, nativeQuery = true)
    List<Content> findNotificationsNextPage(@Param("userId") Long userId,
                                            @Param("lastCheckedAt") LocalDateTime lastCheckedAt,
                                            @Param("lastId") Long lastId,
                                            @Param("size") int size);

    // 읽지 않은 알림 수
    @Query(value = """
    SELECT COUNT(DISTINCT c.content_id)
    FROM content c
    WHERE c.created_at > COALESCE(:lastCheckedAt, '1970-01-01')
    AND (
        c.source_id IN (
            SELECT us.source_id
            FROM user_source us
            WHERE us.user_id = :userId
            AND us.receive_feed = 1
        )
        OR EXISTS (
            SELECT 1 FROM keyword k
            WHERE k.user_id = :userId
            AND k.is_notification_enabled = 1
            AND (
                c.title LIKE CONCAT('%', k.name, '%')
                OR c.summary LIKE CONCAT('%', k.name, '%')
            )
        )
    )
    """, nativeQuery = true)
    long countNotificationsForUser(@Param("userId") Long userId, @Param("lastCheckedAt") LocalDateTime lastCheckedAt);
}
