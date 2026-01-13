package iuh.fit.ottbackend.dto.request;

import iuh.fit.ottbackend.entity.enums.DeviceType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkGoogleRequest {

    private String phone;


    private String googleAccessToken;


    private String otpCode;

    private String deviceId;
    private DeviceType deviceType;
    private String deviceName;
    private String ipAddress;
    private String deviceInfo;
}
