package iuh.fit.ottbackend.dto.response;

import iuh.fit.ottbackend.entity.enums.DeviceType;
import iuh.fit.ottbackend.entity.enums.LoginMethod;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfo {
    private String id;
    private String deviceId;
    private DeviceType deviceType;
    private String deviceName;
    private String ipAddress;
    private String userAgent;
    private LoginMethod loginMethod;
    private Boolean isActive;
    private Boolean isCurrent;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}