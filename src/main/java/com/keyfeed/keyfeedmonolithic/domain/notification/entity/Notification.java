package com.keyfeed.keyfeedmonolithic.domain.notification.entity;

import com.keyfeed.keyfeedmonolithic.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    private Long userId;

    private Long contentId;

    @Column(name = "original_url")
    private String url;

    private String title;

    private String message;

    private boolean isRead;

    public void read() {
        this.isRead = true;
    }
}