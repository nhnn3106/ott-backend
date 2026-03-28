package iuh.fit.se.analyticsservice.repository;

import iuh.fit.se.analyticsservice.entity.RawEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RawEventLog entity
 * 
 * @author OTT Platform Team
 */
@Repository
public interface RawEventLogRepository extends JpaRepository<RawEventLog, UUID> {

    /**
     * Check if event already exists (for idempotency)
     */
    boolean existsByEventId(String eventId);

    /**
     * Find event by event ID
     */
    Optional<RawEventLog> findByEventId(String eventId);

    /**
     * Find unprocessed events by type
     */
    @Query("SELECT e FROM RawEventLog e WHERE e.eventType = :eventType AND e.processed = false ORDER BY e.createdAt ASC")
    List<RawEventLog> findUnprocessedByEventType(@Param("eventType") String eventType);

    /**
     * Find unprocessed events within date range
     */
    @Query("SELECT e FROM RawEventLog e WHERE e.processed = false AND e.createdAt BETWEEN :startDate AND :endDate ORDER BY e.createdAt ASC")
    List<RawEventLog> findUnprocessedByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count unprocessed events
     */
    long countByProcessed(boolean processed);

    /**
     * Find events by user ID
     */
    List<RawEventLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
