package iuh.fit.ottbackend.dto.request;

import iuh.fit.ottbackend.entity.enums.DeviceType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationRequest {

    private String phone;


    private String password;

    private String deviceId;
    private DeviceType deviceType;
    private String deviceName;
    private String ipAddress;
    private String deviceInfo;
}
