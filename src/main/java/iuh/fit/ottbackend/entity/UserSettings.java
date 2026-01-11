package iuh.fit.ottbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {
    @Id
    @Column(name = "user_id")
    private String userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 10)
    private String language = "vi";

    @Column(length = 20)
    private String theme = "light";

    @Column(name = "notification_enabled")
    private Boolean notificationEnabled = true;

    @Column(name = "push_notification_enabled")
    private Boolean pushNotificationEnabled = true;

    @Column(name = "email_notification_enabled")
    private Boolean emailNotificationEnabled = true;

    @Column(name = "show_online_status")
    private Boolean showOnlineStatus = true;

    @Column(name = "show_last_seen")
    private Boolean showLastSeen = true;

    @Column(name = "read_receipts_enabled")
    private Boolean readReceiptsEnabled = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
