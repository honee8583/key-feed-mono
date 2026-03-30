package com.keyfeed.keyfeedmonolithic.infra.notification.repository;

import com.keyfeed.keyfeedmonolithic.infra.notification.entity.NotificationProcessedContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationProcessedContentRepository extends JpaRepository<NotificationProcessedContent, Long> {

    boolean existsByContentId(Long contentId);
}
