package iuh.fit.se.analyticsservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

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
 * Raw Event Log Entity
 * 
 * Event Store for all RabbitMQ events - Single source of truth for analytics.
 * Implements Event Sourcing pattern to avoid querying other services' databases.
 * 
 * Event Types:
 * - USER_REGISTERED: New user registration
 * - USER_LOGIN: Login attempt (success/failure)
 * - SESSION_CREATED: New session creation
 * 
 * @author OTT Platform Team
 */
@Entity
@Table(name = "raw_events_log", indexes = {
    @Index(name = "idx_raw_events_type", columnList = "event_type"),
    @Index(name = "idx_raw_events_processed", columnList = "processed, created_at"),
    @Index(name = "idx_raw_events_event_id", columnList = "event_id"),
    @Index(name = "idx_raw_events_user_id", columnList = "user_id"),
    @Index(name = "idx_raw_events_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Unique event identifier for idempotency checking
     * Prevents duplicate processing of the same event
     */
    @Column(name = "event_id", unique = true, nullable = false, length = 100)
    private String eventId;

    /**
     * Event type classification
     * Values: USER_REGISTERED, USER_LOGIN, SESSION_CREATED
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /**
     * User ID associated with this event
     */
    @Column(name = "user_id")
    private UUID userId;

    /**
     * Full event data stored as JSONB
     * Contains all event details for batch processing
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    /**
     * Processing status flag
     * false: Not yet aggregated into metrics tables
     * true: Already processed by batch jobs
     */
    @Column(name = "processed", nullable = false)
    @Builder.Default
    private Boolean processed = false;

    /**
     * Event creation timestamp (when received from RabbitMQ)
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * When event was processed into metrics
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Mark event as processed
     */
    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
    }
}
