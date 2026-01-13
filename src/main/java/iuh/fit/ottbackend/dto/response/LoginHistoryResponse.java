package iuh.fit.ottbackend.dto.response;

import iuh.fit.ottbackend.entity.enums.DeviceType;
import iuh.fit.ottbackend.entity.enums.LoginMethod;
import iuh.fit.ottbackend.entity.enums.LoginStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistoryResponse {
    private String id;
    private String userId;
    private String ipAddress;
    private DeviceType deviceType;
    private String userAgent;
    private LoginStatus status;
    private LoginMethod loginMethod;
    private String qrCodeId;
    private String failureReason;
    private LocalDateTime createdAt;
}
