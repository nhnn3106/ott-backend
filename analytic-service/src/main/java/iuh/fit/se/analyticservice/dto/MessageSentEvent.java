package iuh.fit.se.analyticservice.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageSentEvent {
    private String eventId;
    private String messageId;
    private String userId;
    private String messageType;
    private Instant timestamp;
}
