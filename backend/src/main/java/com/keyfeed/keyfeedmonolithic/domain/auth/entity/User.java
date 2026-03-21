package com.keyfeed.keyfeedmonolithic.domain.auth.entity;

import com.keyfeed.keyfeedmonolithic.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "`user`")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(length = 120)
    private String email;

    private String password;

    @Column(length = 60)
    private String username;

    @Builder.Default
    private boolean isWithdraw = false;

    @Builder.Default
    private boolean settingPush = false;

    @Builder.Default
    private boolean isSocial = false;

    private String snsType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

}
