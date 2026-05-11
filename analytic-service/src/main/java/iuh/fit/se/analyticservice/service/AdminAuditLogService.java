package iuh.fit.se.analyticservice.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import iuh.fit.se.analyticservice.dto.AuditLogDTO;
import iuh.fit.se.analyticservice.dto.PaginatedAuditLogsResponse;
import iuh.fit.se.analyticservice.entity.AdminAuditLog;
import iuh.fit.se.analyticservice.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private final AdminAuditLogRepository adminAuditLogRepository;

    public void logAction(String adminId, String actionType, String targetId) {
        AdminAuditLog log = new AdminAuditLog(
                null,
                adminId,
                actionType,
                targetId,
                Instant.now()
        );
        adminAuditLogRepository.save(log);
    }

    public PaginatedAuditLogsResponse getLogs(int page, int size) {
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);
        int safePage = Math.max(page, 0);
        Page<AdminAuditLog> result = adminAuditLogRepository.findAll(
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "timestamp"))
        );

        List<AuditLogDTO> items = result.getContent().stream()
                .map(log -> new AuditLogDTO(
                        log.getId(),
                        log.getAdminId(),
                        log.getActionType(),
                        log.getTargetId(),
                        log.getTimestamp()
                ))
                .toList();

        return new PaginatedAuditLogsResponse(
                items,
                result.getTotalElements(),
                result.getNumber(),
                result.getSize(),
                result.getTotalPages()
        );
    }
}
