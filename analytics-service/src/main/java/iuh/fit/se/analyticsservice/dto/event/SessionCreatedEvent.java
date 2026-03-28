package iuh.fit.se.analyticsservice.dto.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import java.io.Serializable;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.NoArgsConstructor;

/**
 * Session Created Event
 * 
 * Published by User Service when a new session is created.
 * Consumed by Analytics Service to track active users and sessions.
 * 
 * @author OTT Platform Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionCreatedEvent implements Serializable {
    
    /**
     * Unique event identifier for idempotency
     */
    private String eventId;
    
    /**
     * User ID who created the session
     */
    private UUID userId;
    
    /**
     * Session ID
     */
    private UUID sessionId;
    
    /**
     * Device type: MOBILE, DESKTOP, TABLET, TV, UNKNOWN
     */
    private String deviceType;
    
    /**
     * Login method: LOCAL, GOOGLE, OTP, QR_CODE
     */
    private String loginMethod;
    
    /**
     * Client IP address
     */
    private String ipAddress;
    
    /**
     * Geographic location (parsed from IP)
     */
    private String location;
    
    /**
     * Session creation timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
