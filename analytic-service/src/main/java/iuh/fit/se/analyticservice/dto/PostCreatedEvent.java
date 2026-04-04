package iuh.fit.se.analyticservice.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostCreatedEvent {
    private String eventId;
    private String postId;
    private String userId;
    private Instant timestamp;
}
