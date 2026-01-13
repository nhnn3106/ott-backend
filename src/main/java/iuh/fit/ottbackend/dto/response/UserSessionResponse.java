package iuh.fit.ottbackend.dto.response;

import iuh.fit.ottbackend.entity.enums.DeviceType;
import iuh.fit.ottbackend.entity.enums.LoginMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionResponse {
    private String id;
    private String userId;
    private String deviceId;
    private DeviceType deviceType;
    private String deviceName;
    private String ipAddress;
    private String userAgent;
    private LoginMethod loginMethod;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private String revokedReason;
}
