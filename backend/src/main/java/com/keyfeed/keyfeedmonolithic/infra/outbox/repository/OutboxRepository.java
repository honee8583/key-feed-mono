package com.keyfeed.keyfeedmonolithic.infra.outbox.repository;

import com.keyfeed.keyfeedmonolithic.infra.outbox.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    @Query("""
        SELECT o FROM Outbox o
        WHERE o.status IN ('PENDING', 'FAILED')
          AND o.retryCount < o.maxRetry
          AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now)
        ORDER BY o.createdAt ASC
        LIMIT 100
    """)
    List<Outbox> findPendingEvents(@Param("now") LocalDateTime now);
}
