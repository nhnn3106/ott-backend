package iuh.fit.se.analyticservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginEvent {
    private String eventId;
    private String userId;
    private String loginMethod;
    private Instant timestamp;
}
