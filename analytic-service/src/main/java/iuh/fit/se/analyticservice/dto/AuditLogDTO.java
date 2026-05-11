package iuh.fit.se.analyticservice.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private Long id;
    private String adminId;
    private String actionType;
    private String targetId;
    private Instant timestamp;
}
