package iuh.fit.se.analyticsservice.entity;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;

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
 * Geographic Analytics Entity
 * 
 * Stores geographic distribution statistics by date.
 * Updated by scheduled job daily at 1:45 AM.
 * 
 * @author OTT Platform Team
 */
@Entity
@Table(name = "geographic_analytics", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"date", "country", "city"}),
    indexes = {
        @Index(name = "idx_geo_analytics_date", columnList = "date"),
        @Index(name = "idx_geo_analytics_country", columnList = "country")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeographicAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "city", length = 200)
    private String city;

    @Column(name = "total_sessions", nullable = false)
    @Builder.Default
    private Integer totalSessions = 0;

    @Column(name = "unique_users", nullable = false)
    @Builder.Default
    private Integer uniqueUsers = 0;

    @Column(name = "total_logins", nullable = false)
    @Builder.Default
    private Integer totalLogins = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
