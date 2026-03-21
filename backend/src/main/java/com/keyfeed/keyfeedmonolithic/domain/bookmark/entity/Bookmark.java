package com.keyfeed.keyfeedmonolithic.domain.bookmark.entity;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "bookmark",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bookmark_user_content",
                        columnNames = {"user_id", "content_id"}
                )
        }
)
public class Bookmark extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "content_id", nullable = false)
    private String contentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookmark_folder_id")
    private BookmarkFolder bookmarkFolder;

    public void removeFolder() {
        this.bookmarkFolder = null;
    }

    public void changeFolder(BookmarkFolder folder) {
        this.bookmarkFolder = folder;
    }
}
