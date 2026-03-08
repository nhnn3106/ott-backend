package iuh.fit.ottbackend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleTokenAuthRequest {
    private String idToken;
    private String accessToken;

    private String deviceId;
    private String deviceType;
    private String deviceName;
    private String deviceInfo;
    private String ipAddress;
    private String location;
}