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
public class QrStatusResponse {
    private String qrId;
    private String status; // PENDING, SCANNED, CONFIRMED, EXPIRED, CANCELLED
    private String message;

    // Device info (for mobile to show what device is trying to login)
    private String deviceInfo;
    private String ipAddress;
    private String location;

    // Session info (only when CONFIRMED)
    private String sessionToken;
    private String refreshToken;
    private LocalDateTime expiresAt;
}
