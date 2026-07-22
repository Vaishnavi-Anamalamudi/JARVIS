package com.adaptivegateway.auth.repository;

import com.adaptivegateway.auth.entity.LoginAuditLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAuditLogRepository extends JpaRepository<LoginAuditLog, UUID> {
}
