package iuh.fit.notificationservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeEmailEvent {
    private String toEmail;
    private String toName;
    private String userId;
}
