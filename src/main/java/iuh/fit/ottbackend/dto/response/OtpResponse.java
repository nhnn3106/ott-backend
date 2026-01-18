package iuh.fit.ottbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpResponse {
    private String phone;
    private String message;
    private String email;
    private LocalDateTime expiresAt;
    private int expiryMinutes;
    private int remainingAttempts;
}
