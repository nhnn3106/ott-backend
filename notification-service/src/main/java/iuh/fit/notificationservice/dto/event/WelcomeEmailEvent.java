package iuh.fit.notificationservice.dto.event;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeEmailEvent {
    private String toEmail;
    private String toName;
    private String phone;
    private boolean hasPassword;
    private boolean hasGoogleLinked;
    private String userId;
}