package com.keyfeed.keyfeedmonolithic.domain.notification.repository;

import com.keyfeed.keyfeedmonolithic.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Last-Event-ID 이후에 생성된 알림 조회
    List<Notification> findAllByUserIdAndIdGreaterThan(Long userId, Long id);

    // 알림 첫 페이지 조회
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.id DESC")
    List<Notification> findFirstPage(@Param("userId") Long userId, Pageable pageable);

    // 다음 커서 알림 조회
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.id < :lastId ORDER BY n.id DESC")
    List<Notification> findNextPage(@Param("userId") Long userId, @Param("lastId") Long lastId, Pageable pageable);

}