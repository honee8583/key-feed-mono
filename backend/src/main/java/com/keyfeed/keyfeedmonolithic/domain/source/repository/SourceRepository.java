package com.keyfeed.keyfeedmonolithic.domain.source.repository;

import com.keyfeed.keyfeedmonolithic.domain.source.entity.Source;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SourceRepository extends JpaRepository<Source, Long> {

    Optional<Source> findByUrl(String url);

    @Query("SELECT s FROM Source s WHERE s.lastCrawledAt IS NULL OR s.lastCrawledAt < :targetTime")
    List<Source> findSourcesToCrawl(@Param("targetTime") LocalDateTime targetTime);

    @Query("""
            SELECT s.id as sourceId, s.url as url, COUNT(DISTINCT us.user.id) as subscriberCount
            FROM Source s
            JOIN UserSource us ON s.id = us.source.id
            JOIN Keyword k ON us.user.id = k.user.id
            WHERE k.name IN :keywordNames
            AND us.user.id != :userId
            AND NOT EXISTS (
                SELECT 1 FROM UserSource us2
                WHERE us2.source.id = s.id AND us2.user.id = :userId
            )
            GROUP BY s.id, s.url
            ORDER BY subscriberCount DESC
            """)
    List<RecommendedSourceProjection> findRecommendedSourcesByKeywords(
            @Param("keywordNames") List<String> keywordNames,
            @Param("userId") Long userId,
            Pageable pageable);

    interface RecommendedSourceProjection {
        Long getSourceId();
        String getUrl();
        Long getSubscriberCount();
    }

}
