package com.keyfeed.keyfeedmonolithic.domain.keyword.repository;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.keyword.entity.Keyword;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    List<Keyword> findByUserId(Long userId);

    Optional<Keyword> findByIdAndUserId(Long keywordId, Long userId);

    boolean existsByNameAndUser(String name, User user);

    Long countByUserId(Long userId);

    Long countByUserIdAndIsEnabledTrue(Long userId);

    List<Keyword> findByUserIdOrderByCreatedAtAsc(Long userId);

    @Modifying
    @Query("UPDATE Keyword k SET k.isEnabled = true WHERE k.user.id = :userId AND k.isEnabled = false")
    void enableAllByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT k.user.id " +
            "FROM Keyword k " +
            "JOIN UserSource us ON k.user.id = us.user.id " +
            "WHERE k.name IN :keywords " +
            "AND us.source.id = :sourceId " +
            "AND k.isNotificationEnabled = true " +
            "AND k.isEnabled = true")
    List<Long> findUserIdsByNamesAndSourceId(@Param("keywords") Set<String> keywords, @Param("sourceId") Long sourceId);

    @Query(value = """
    SELECT DISTINCT k.user_id
    FROM keyword k
    INNER JOIN user_source us ON k.user_id = us.user_id
    WHERE k.name IN :keywords
    AND us.source_id = :sourceId
    AND k.is_notification_enabled = 1
    AND k.is_enabled = 1
    AND us.receive_feed = 1
    """, nativeQuery = true)
    List<Long> findUserIdsByNamesAndSourceId(
            @Param("keywords") Set<String> keywords,
            @Param("sourceId") Long sourceId,
            Pageable pageable
    );

    @Query(value = """
    SELECT DISTINCT k.user_id
    FROM keyword k
    INNER JOIN user_source us ON k.user_id = us.user_id
    WHERE k.name IN :keywords
    AND us.source_id = :sourceId
    AND k.is_notification_enabled = 1
    AND k.is_enabled = 1
    AND us.receive_feed = 1
    AND k.user_id > :lastUserId
    ORDER BY k.user_id ASC
    LIMIT :chunkSize
    """, nativeQuery = true)
    List<Long> findUserIdsByNamesAndSourceId(
            @Param("keywords") Set<String> keywords,
            @Param("sourceId") Long sourceId,
            @Param("lastUserId") Long lastUserId,
            @Param("chunkSize") int chunkSize
    );

    @Modifying
    @Query("DELETE FROM Keyword k WHERE k.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT k.name AS name, COUNT(DISTINCT k.user.id) AS userCount
            FROM Keyword k
            GROUP BY k.name
            ORDER BY userCount DESC, k.name ASC
            """)
    List<TrendingKeywordProjection> findTrendingKeywords(Pageable pageable);

}
