package iuh.fit.se.analyticsservice.dto.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Login Event
 * 
 * Published by Auth Service after each login attempt.
 * Consumed by Analytics Service to track login statistics.
 * 
 * @author OTT Platform Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginEvent implements Serializable {
    
    /**
     * Unique event identifier for idempotency
     */
    private String eventId;
    
    /**
     * User ID who attempted login
     */
    private UUID userId;
    
    /**
     * Login method: LOCAL, GOOGLE, OTP, QR_CODE
     */
    private String loginMethod;
    
    /**
     * Login status: SUCCESS, FAILED, BLOCKED, REQUIRES_2FA
     */
    private String status;
    
    /**
     * Device type: MOBILE, DESKTOP, TABLET, TV, UNKNOWN
     */
    private String deviceType;
    
    /**
     * Client IP address
     */
    private String ipAddress;
    
    /**
     * Geographic location (parsed from IP)
     */
    private String location;
    
    /**
     * Failure reason (if status is FAILED)
     */
    private String failureReason;
    
    /**
     * Login attempt timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
