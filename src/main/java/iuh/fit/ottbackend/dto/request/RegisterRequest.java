package iuh.fit.ottbackend.dto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String phone;
    private String password;
    private String fullName;
    private String otp;
    private String ipAddress;
}