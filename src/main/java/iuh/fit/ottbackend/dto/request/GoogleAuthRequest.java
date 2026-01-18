package iuh.fit.ottbackend.dto.request;

import iuh.fit.ottbackend.entity.enums.DeviceType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleAuthRequest {
    private String code;
    private String redirectUri;
    private String deviceId;
    private DeviceType deviceType;
    private String deviceName;
    private String ipAddress;
    private String deviceInfo;
}
