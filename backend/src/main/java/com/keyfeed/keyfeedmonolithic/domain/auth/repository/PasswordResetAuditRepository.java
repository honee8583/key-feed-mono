package com.keyfeed.keyfeedmonolithic.domain.auth.repository;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.PasswordResetAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetAuditRepository extends JpaRepository<PasswordResetAudit, Long> {
}
