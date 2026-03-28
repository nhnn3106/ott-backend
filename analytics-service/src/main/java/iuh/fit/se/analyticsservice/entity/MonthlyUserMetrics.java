package iuh.fit.se.analyticsservice.entity;

import java.math.BigDecimal;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Monthly User Metrics Entity
 * 
 * Stores aggregated monthly statistics.
 * Updated by scheduled job on 1st of each month at 2:00 AM.
 * 
 * @author OTT Platform Team
 */
@Entity
@Table(name = "monthly_user_metrics", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"year", "month"}),
    indexes = @Index(name = "idx_monthly_metrics_year_month", columnList = "year, month"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyUserMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    /**
     * MAU - Monthly Active Users
     */
    @Column(name = "monthly_active_users", nullable = false)
    @Builder.Default
    private Integer monthlyActiveUsers = 0;

    @Column(name = "new_registrations", nullable = false)
    @Builder.Default
    private Integer newRegistrations = 0;

    /**
     * Users who were active last month but not this month
     */
    @Column(name = "churned_users", nullable = false)
    @Builder.Default
    private Integer churnedUsers = 0;

    /**
     * Retention rate percentage
     */
    @Column(name = "retention_rate", precision = 5, scale = 2)
    private BigDecimal retentionRate;

    @Column(name = "avg_sessions_per_user", precision = 10, scale = 2)
    private BigDecimal avgSessionsPerUser;

    @Column(name = "avg_session_duration_minutes", precision = 10, scale = 2)
    private BigDecimal avgSessionDurationMinutes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
