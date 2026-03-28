package iuh.fit.se.analyticsservice.entity;

import org.hibernate.annotations.CreationTimestamp;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Retention Cohort Entity
 * 
 * Tracks user retention by registration cohort.
 * Updated by scheduled job daily at 3:00 AM.
 * 
 * @author OTT Platform Team
 */
@Entity
@Table(name = "retention_cohort", indexes = {
    @Index(name = "idx_retention_cohort_date", columnList = "cohort_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetentionCohort {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Registration date (cohort identifier)
     */
    @Column(name = "cohort_date", unique = true, nullable = false)
    private LocalDate cohortDate;

    /**
     * Number of users who registered on cohort_date
     */
    @Column(name = "cohort_size", nullable = false)
    private Integer cohortSize;

    /**
     * Users active on day 0 (registration day)
     */
    @Column(name = "day_0_users", nullable = false)
    private Integer day0Users;

    @Column(name = "day_1_users")
    private Integer day1Users;

    @Column(name = "day_7_users")
    private Integer day7Users;

    @Column(name = "day_30_users")
    private Integer day30Users;

    @Column(name = "day_60_users")
    private Integer day60Users;

    @Column(name = "day_90_users")
    private Integer day90Users;

    /**
     * Retention rates (percentage)
     */
    @Column(name = "retention_1_day", precision = 5, scale = 2)
    private BigDecimal retention1Day;

    @Column(name = "retention_7_day", precision = 5, scale = 2)
    private BigDecimal retention7Day;

    @Column(name = "retention_30_day", precision = 5, scale = 2)
    private BigDecimal retention30Day;

    @Column(name = "retention_60_day", precision = 5, scale = 2)
    private BigDecimal retention60Day;

    @Column(name = "retention_90_day", precision = 5, scale = 2)
    private BigDecimal retention90Day;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
