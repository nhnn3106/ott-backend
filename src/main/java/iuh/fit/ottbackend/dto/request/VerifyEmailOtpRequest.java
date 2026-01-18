package iuh.fit.ottbackend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyEmailOtpRequest {

    private String email;

    private String otpCode;

    // Device info for creating session
    private String deviceId;
    private String deviceType;
    private String deviceName;
    private String ipAddress;
    private String deviceInfo;
}
