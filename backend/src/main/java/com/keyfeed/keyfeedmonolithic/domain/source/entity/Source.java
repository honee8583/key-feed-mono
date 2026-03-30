package com.keyfeed.keyfeedmonolithic.domain.source.entity;

import com.keyfeed.keyfeedmonolithic.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Source extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "source_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 767)
    private String url;

    @Column(name = "last_crawled_at")
    private LocalDateTime lastCrawledAt;

    @Column(name = "last_item_hash")
    private String lastItemHash;

    @Column(name = "logo_url", length = 2048)
    private String logoUrl;

    public void updateLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void updateLastCrawledAt(LocalDateTime lastCrawledAt) {
        this.lastCrawledAt = lastCrawledAt;
    }

    public void updateLastItemHash(String lastItemHash) {
        this.lastItemHash = lastItemHash;
    }

}
