package iuh.fit.ottbackend.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorRequiredResponse {
    private String tempToken;
    private String maskedEmail;
    private LocalDateTime expiresAt;
    private String message;
}