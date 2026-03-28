package iuh.fit.se.analyticsservice.entity;

import java.math.BigDecimal;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Device Analytics Entity
 * 
 * Stores device type distribution statistics by date.
 * Updated by scheduled job daily at 1:30 AM.
 * 
 * @author OTT Platform Team
 */
@Entity
@Table(name = "device_analytics", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"date", "device_type"}),
    indexes = @Index(name = "idx_device_analytics_date", columnList = "date, device_type"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    /**
     * Device type: MOBILE, DESKTOP, TABLET, TV, UNKNOWN
     */
    @Column(name = "device_type", nullable = false, length = 50)
    private String deviceType;

    @Column(name = "total_sessions", nullable = false)
    @Builder.Default
    private Integer totalSessions = 0;

    @Column(name = "unique_users", nullable = false)
    @Builder.Default
    private Integer uniqueUsers = 0;

    @Column(name = "total_logins", nullable = false)
    @Builder.Default
    private Integer totalLogins = 0;

    @Column(name = "avg_session_duration_minutes", precision = 10, scale = 2)
    private BigDecimal avgSessionDurationMinutes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
