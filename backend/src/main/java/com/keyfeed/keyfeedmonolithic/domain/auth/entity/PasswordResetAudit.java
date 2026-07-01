package com.keyfeed.keyfeedmonolithic.domain.auth.entity;

import com.keyfeed.keyfeedmonolithic.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "password_reset_audit")
public class PasswordResetAudit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 120, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PasswordResetAuditType type;

    public static PasswordResetAudit of(String email, PasswordResetAuditType type) {
        return PasswordResetAudit.builder()
                .email(email)
                .type(type)
                .build();
    }
}
