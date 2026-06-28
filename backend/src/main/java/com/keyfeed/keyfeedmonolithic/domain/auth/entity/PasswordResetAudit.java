package com.keyfeed.keyfeedmonolithic.domain.auth.entity;

import com.keyfeed.keyfeedmonolithic.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 비밀번호 재설정 등 민감 인증 작업의 이력. 인증 코드 저장소가 Redis로 전환되며
 * 휘발되는 인증 이력을 영구 보존하기 위한 감사(audit) 레코드.
 */
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
