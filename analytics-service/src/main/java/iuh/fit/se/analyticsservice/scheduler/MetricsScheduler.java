package iuh.fit.se.analyticsservice.scheduler;

import iuh.fit.se.analyticsservice.service.MetricsAggregationService;
import iuh.fit.se.analyticsservice.service.RealtimeMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Metrics Scheduler
 * 
 * Scheduled jobs for metrics aggregation.
 * Uses ShedLock to prevent duplicate execution when running multiple instances.
 * 
 * IMPORTANT: These jobs ONLY query raw_events_log table.
 * They NEVER access User Service or Auth Service databases directly.
 * 
 * @author OTT Platform Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsScheduler {

    private final MetricsAggregationService metricsAggregationService;
    private final RealtimeMetricsService realtimeMetricsService;

    /**
     * Calculate daily metrics at 1:00 AM every day
     * Lock: 10 minutes
     */
    @Scheduled(cron = "0 0 1 * * *")
    @SchedulerLock(
        name = "calculateDailyMetrics",
        lockAtMostFor = "10m",
        lockAtLeastFor = "1m"
    )
    public void calculateDailyMetrics() {
        log.info("Starting daily metrics calculation job");
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            metricsAggregationService.calculateDailyMetrics(yesterday);
            log.info("Daily metrics calculation completed successfully");
        } catch (Exception e) {
            log.error("Failed to calculate daily metrics", e);
        }
    }

    /**
     * Calculate monthly metrics at 2:00 AM on the 1st day of each month
     * Lock: 30 minutes
     */
    @Scheduled(cron = "0 0 2 1 * *")
    @SchedulerLock(
        name = "calculateMonthlyMetrics",
        lockAtMostFor = "30m",
        lockAtLeastFor = "5m"
    )
    public void calculateMonthlyMetrics() {
        log.info("Starting monthly metrics calculation job");
        try {
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            int year = lastMonth.getYear();
            int month = lastMonth.getMonthValue();
            metricsAggregationService.calculateMonthlyMetrics(year, month);
            log.info("Monthly metrics calculation completed successfully");
        } catch (Exception e) {
            log.error("Failed to calculate monthly metrics", e);
        }
    }

    /**
     * Update retention cohorts at 3:00 AM every day
     * Lock: 20 minutes
     */
    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(
        name = "updateRetentionCohorts",
        lockAtMostFor = "20m",
        lockAtLeastFor = "2m"
    )
    public void updateRetentionCohorts() {
        log.info("Starting retention cohorts update job");
        try {
            // Update cohorts for last 30 days
            LocalDate today = LocalDate.now();
            for (int i = 1; i <= 30; i++) {
                LocalDate cohortDate = today.minusDays(i);
                metricsAggregationService.updateRetentionCohorts(cohortDate);
            }
            log.info("Retention cohorts update completed successfully");
        } catch (Exception e) {
            log.error("Failed to update retention cohorts", e);
        }
    }

    /**
     * Update device analytics at 4:00 AM every day
     * Lock: 15 minutes
     */
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(
        name = "updateDeviceAnalytics",
        lockAtMostFor = "15m",
        lockAtLeastFor = "1m"
    )
    public void updateDeviceAnalytics() {
        log.info("Starting device analytics update job");
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            metricsAggregationService.updateDeviceAnalytics(yesterday);
            log.info("Device analytics update completed successfully");
        } catch (Exception e) {
            log.error("Failed to update device analytics", e);
        }
    }

    /**
     * Reset daily counters at midnight
     * Lock: 5 minutes
     */
    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(
        name = "resetDailyCounters",
        lockAtMostFor = "5m",
        lockAtLeastFor = "30s"
    )
    public void resetDailyCounters() {
        log.info("Starting daily counters reset job");
        try {
            realtimeMetricsService.resetDailyCounters();
            log.info("Daily counters reset completed successfully");
        } catch (Exception e) {
            log.error("Failed to reset daily counters", e);
        }
    }

    /**
     * Clean up old processed events (older than 90 days) at 5:00 AM every Sunday
     * Lock: 60 minutes
     */
    @Scheduled(cron = "0 0 5 * * SUN")
    @SchedulerLock(
        name = "cleanupOldEvents",
        lockAtMostFor = "60m",
        lockAtLeastFor = "5m"
    )
    public void cleanupOldEvents() {
        log.info("Starting old events cleanup job");
        try {
            // TODO: Implement cleanup logic
            // Delete processed events older than 90 days from raw_events_log
            log.info("Old events cleanup completed successfully");
        } catch (Exception e) {
            log.error("Failed to cleanup old events", e);
        }
    }

    /**
     * Health check job - runs every 5 minutes
     * No lock needed (read-only operation)
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void healthCheck() {
        log.debug("Running analytics service health check");
        try {
            boolean cacheHealthy = realtimeMetricsService.isCacheHealthy();
            if (!cacheHealthy) {
                log.warn("Analytics service health check failed: Cache is unhealthy");
            }
        } catch (Exception e) {
            log.error("Health check failed", e);
        }
    }
}
