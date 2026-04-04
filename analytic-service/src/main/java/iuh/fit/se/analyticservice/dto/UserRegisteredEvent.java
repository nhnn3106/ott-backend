package iuh.fit.se.analyticservice.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
    private String eventId;
    private String userId;
    private String registerMethod;
    private Instant timestamp;
}
