package iuh.fit.se.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Login Method Analytics Entity
 * 
 * Stores login method distribution and success rates by date.
 * Updated by scheduled job daily at 1:00 AM.
 * 
 * @author OTT Platform Team
 */
@Entity
@Table(name = "login_method_analytics", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"date", "login_method"}),
    indexes = @Index(name = "idx_login_method_date", columnList = "date"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginMethodAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    /**
     * Login method: LOCAL, GOOGLE, OTP, QR_CODE
     */
    @Column(name = "login_method", nullable = false, length = 50)
    private String loginMethod;

    @Column(name = "total_attempts", nullable = false)
    @Builder.Default
    private Integer totalAttempts = 0;

    @Column(name = "successful_attempts", nullable = false)
    @Builder.Default
    private Integer successfulAttempts = 0;

    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private Integer failedAttempts = 0;

    @Column(name = "unique_users", nullable = false)
    @Builder.Default
    private Integer uniqueUsers = 0;

    /**
     * Success rate percentage
     */
    @Column(name = "success_rate", precision = 5, scale = 2)
    private BigDecimal successRate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
