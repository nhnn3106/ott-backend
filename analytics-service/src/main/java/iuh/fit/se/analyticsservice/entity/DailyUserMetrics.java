package iuh.fit.se.analyticsservice.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Daily User Metrics Entity
 * 
 * Stores aggregated daily statistics computed from raw_events_log.
 * Updated by scheduled job daily at 1:00 AM.
 * 
 * @author OTT Platform Team
 */
@Entity
@Table(name = "daily_user_metrics", indexes = {
    @Index(name = "idx_daily_metrics_date", columnList = "date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyUserMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Metrics date (UNIQUE constraint)
     */
    @Column(name = "date", unique = true, nullable = false)
    private LocalDate date;

    /**
     * Number of new user registrations on this date
     */
    @Column(name = "new_registrations", nullable = false)
    @Builder.Default
    private Integer newRegistrations = 0;

    /**
     * DAU - Daily Active Users (users who logged in)
     */
    @Column(name = "daily_active_users", nullable = false)
    @Builder.Default
    private Integer dailyActiveUsers = 0;

    /**
     * Total cumulative users in system
     */
    @Column(name = "total_users", nullable = false)
    @Builder.Default
    private Integer totalUsers = 0;

    /**
     * Users with verified phone/email
     */
    @Column(name = "verified_users", nullable = false)
    @Builder.Default
    private Integer verifiedUsers = 0;

    /**
     * Users registered via Google OAuth
     */
    @Column(name = "google_users", nullable = false)
    @Builder.Default
    private Integer googleUsers = 0;

    /**
     * Users registered via local (phone/email + password)
     */
    @Column(name = "local_users", nullable = false)
    @Builder.Default
    private Integer localUsers = 0;

    /**
     * Users with 2FA enabled
     */
    @Column(name = "two_fa_enabled_users", nullable = false)
    @Builder.Default
    private Integer twoFaEnabledUsers = 0;

    /**
     * Users currently blocked
     */
    @Column(name = "blocked_users", nullable = false)
    @Builder.Default
    private Integer blockedUsers = 0;

    /**
     * Users who soft-deleted their accounts
     */
    @Column(name = "deleted_users", nullable = false)
    @Builder.Default
    private Integer deletedUsers = 0;

    /**
     * Total login attempts (success + failure)
     */
    @Column(name = "total_logins", nullable = false)
    @Builder.Default
    private Integer totalLogins = 0;

    /**
     * Successful login attempts
     */
    @Column(name = "successful_logins", nullable = false)
    @Builder.Default
    private Integer successfulLogins = 0;

    /**
     * Failed login attempts
     */
    @Column(name = "failed_logins", nullable = false)
    @Builder.Default
    private Integer failedLogins = 0;

    /**
     * Distinct users with login attempts
     */
    @Column(name = "unique_login_users", nullable = false)
    @Builder.Default
    private Integer uniqueLoginUsers = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
