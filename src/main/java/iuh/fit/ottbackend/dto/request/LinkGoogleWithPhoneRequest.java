package iuh.fit.ottbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkGoogleWithPhoneRequest {
    @NotBlank(message = "Google authorization code is required")
    private String code;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "OTP is required")
    private String otp;

    private String redirectUri;
    private String deviceId;
    private String deviceType;
    private String deviceName;
    private String deviceInfo;
    private String ipAddress;
    private String location;
}