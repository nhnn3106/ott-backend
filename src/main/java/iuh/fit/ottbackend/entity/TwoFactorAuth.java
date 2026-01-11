package iuh.fit.ottbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "two_factor_auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorAuth {

    @Id
    @Column(name = "user_id")
    private String userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "is_enabled")
    private Boolean isEnabled = false;

    @Column(name = "secret_key")
    private String secretKey;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "two_factor_backup_codes",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "backup_code")
    private List<String> backupCodes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

