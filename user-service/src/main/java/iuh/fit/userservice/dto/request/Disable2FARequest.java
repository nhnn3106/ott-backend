package iuh.fit.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Disable2FARequest {
    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "OTP is required")
    private String otp;
}