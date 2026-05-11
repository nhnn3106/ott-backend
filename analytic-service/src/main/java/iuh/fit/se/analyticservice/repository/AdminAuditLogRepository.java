package iuh.fit.se.analyticservice.repository;

import iuh.fit.se.analyticservice.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
}
