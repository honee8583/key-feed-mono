package com.keyfeed.keyfeedmonolithic.domain.keyword.entity;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Keyword extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keyword_id")
    private Long id;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String name;

    @Builder.Default
    private boolean isNotificationEnabled = true;

    public void setNotificationEnabled(boolean isNotificationEnabled) {
        this.isNotificationEnabled = isNotificationEnabled;
    }

}
