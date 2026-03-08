package iuh.fit.notificationservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpEmailRequest {
    private String toEmail;
    private String toName;
    private String otpCode;
    private String otpType;
    private String ipAddress;
    private String location;
    private String userId;
}
