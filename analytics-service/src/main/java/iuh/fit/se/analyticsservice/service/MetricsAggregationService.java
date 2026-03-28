package iuh.fit.se.analyticsservice.service;

import iuh.fit.se.analyticsservice.entity.*;
import iuh.fit.se.analyticsservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Metrics Aggregation Service
 * 
 * Aggregates metrics FROM raw_events_log table ONLY.
 * NEVER queries User Service or Auth Service databases directly.
 * This service follows microservices principle by using event sourcing.
 * 
 * @author OTT Platform Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsAggregationService {

    private final RawEventLogRepository rawEventLogRepository;
    private final DailyUserMetricsRepository dailyUserMetricsRepository;
    private final MonthlyUserMetricsRepository monthlyUserMetricsRepository;
    private final RetentionCohortRepository retentionCohortRepository;
    private final DeviceAnalyticsRepository deviceAnalyticsRepository;
    private final GeographicAnalyticsRepository geographicAnalyticsRepository;
    private final LoginMethodAnalyticsRepository loginMethodAnalyticsRepository;

    /**
     * Calculate daily metrics from raw events
     * Called by scheduled job at 1 AM daily
     */
    @Transactional
    public DailyUserMetrics calculateDailyMetrics(LocalDate date) {
        log.info("Calculating daily metrics for date: {}", date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        // Get all unprocessed events for the date
        List<RawEventLog> events = rawEventLogRepository.findUnprocessedByDateRange(startOfDay, endOfDay);

        // Count new registrations
        long newRegistrations = events.stream()
            .filter(e -> "USER_REGISTERED".equals(e.getEventType()))
            .count();

        // Count unique active users (from SESSION_CREATED events)
        long dailyActiveUsers = events.stream()
            .filter(e -> "SESSION_CREATED".equals(e.getEventType()))
            .map(RawEventLog::getUserId)
            .distinct()
            .count();

        // Count total sessions
        long totalSessions = events.stream()
            .filter(e -> "SESSION_CREATED".equals(e.getEventType()))
            .count();

        // Count successful logins
        long successfulLogins = events.stream()
            .filter(e -> "USER_LOGIN".equals(e.getEventType()))
            .filter(e -> {
                String status = e.getPayload().path("status").asText();
                return "SUCCESS".equals(status);
            })
            .count();

        // Count failed logins
        long failedLogins = events.stream()
            .filter(e -> "USER_LOGIN".equals(e.getEventType()))
            .filter(e -> {
                String status = e.getPayload().path("status").asText();
                return "FAILED".equals(status);
            })
            .count();

        // Get previous day metrics for comparison
        DailyUserMetrics previousDay = dailyUserMetricsRepository
            .findByDate(date.minusDays(1))
            .orElse(null);

        // Create or update daily metrics
        DailyUserMetrics metrics = dailyUserMetricsRepository
            .findByDate(date)
            .orElse(DailyUserMetrics.builder()
                .date(date)
                .build());

        metrics.setNewRegistrations(newRegistrations);
        metrics.setDailyActiveUsers(dailyActiveUsers);
        metrics.setTotalSessions(totalSessions);
        metrics.setSuccessfulLogins(successfulLogins);
        metrics.setFailedLogins(failedLogins);

        // Calculate growth rate
        if (previousDay != null && previousDay.getNewRegistrations() > 0) {
            double growthRate = ((double) (newRegistrations - previousDay.getNewRegistrations()) 
                / previousDay.getNewRegistrations()) * 100;
            metrics.setGrowthRate(growthRate);
        }

        DailyUserMetrics saved = dailyUserMetricsRepository.save(metrics);

        // Mark events as processed
        events.forEach(e -> e.setProcessed(true));
        rawEventLogRepository.saveAll(events);

        log.info("Daily metrics calculated: date={}, newRegistrations={}, DAU={}", 
            date, newRegistrations, dailyActiveUsers);

        return saved;
    }

    /**
     * Calculate monthly metrics from daily metrics
     * Called by scheduled job at 2 AM on the first day of month
     */
    @Transactional
    public MonthlyUserMetrics calculateMonthlyMetrics(int year, int month) {
        log.info("Calculating monthly metrics for: {}-{}", year, month);

        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

        // Aggregate from daily_user_metrics table
        List<DailyUserMetrics> dailyMetrics = dailyUserMetricsRepository
            .findByDateBetweenOrderByDateDesc(startOfMonth, endOfMonth);

        if (dailyMetrics.isEmpty()) {
            log.warn("No daily metrics found for {}-{}", year, month);
            return null;
        }

        long totalRegistrations = dailyMetrics.stream()
            .mapToLong(DailyUserMetrics::getNewRegistrations)
            .sum();

        long avgDau = (long) dailyMetrics.stream()
            .mapToLong(DailyUserMetrics::getDailyActiveUsers)
            .average()
            .orElse(0);

        long totalSessions = dailyMetrics.stream()
            .mapToLong(DailyUserMetrics::getTotalSessions)
            .sum();

        long totalLogins = dailyMetrics.stream()
            .mapToLong(DailyUserMetrics::getSuccessfulLogins)
            .sum();

        // Calculate MAU (unique users who had at least one session in the month)
        LocalDateTime startDateTime = startOfMonth.atStartOfDay();
        LocalDateTime endDateTime = endOfMonth.plusDays(1).atStartOfDay();
        
        long monthlyActiveUsers = rawEventLogRepository
            .findUnprocessedByDateRange(startDateTime, endDateTime)
            .stream()
            .filter(e -> "SESSION_CREATED".equals(e.getEventType()))
            .map(RawEventLog::getUserId)
            .distinct()
            .count();

        MonthlyUserMetrics metrics = monthlyUserMetricsRepository
            .findByYearAndMonth(year, month)
            .orElse(MonthlyUserMetrics.builder()
                .year(year)
                .month(month)
                .build());

        metrics.setTotalRegistrations(totalRegistrations);
        metrics.setMonthlyActiveUsers(monthlyActiveUsers);
        metrics.setAverageDailyActiveUsers(avgDau);
        metrics.setTotalSessions(totalSessions);
        metrics.setTotalLogins(totalLogins);

        MonthlyUserMetrics saved = monthlyUserMetricsRepository.save(metrics);

        log.info("Monthly metrics calculated: {}-{}, registrations={}, MAU={}", 
            year, month, totalRegistrations, monthlyActiveUsers);

        return saved;
    }

    /**
     * Update retention cohorts
     * Called by scheduled job daily
     */
    @Transactional
    public void updateRetentionCohorts(LocalDate cohortDate) {
        log.info("Updating retention cohort for date: {}", cohortDate);

        // Get users who registered on cohort date
        LocalDateTime startOfDay = cohortDate.atStartOfDay();
        LocalDateTime endOfDay = cohortDate.plusDays(1).atStartOfDay();

        List<UUID> cohortUsers = rawEventLogRepository
            .findUnprocessedByDateRange(startOfDay, endOfDay)
            .stream()
            .filter(e -> "USER_REGISTERED".equals(e.getEventType()))
            .map(RawEventLog::getUserId)
            .distinct()
            .collect(Collectors.toList());

        if (cohortUsers.isEmpty()) {
            log.info("No users registered on {}, skipping cohort", cohortDate);
            return;
        }

        int cohortSize = cohortUsers.size();

        // Calculate retention for Day 1, 7, 30
        long day1Retained = calculateRetainedUsers(cohortUsers, cohortDate.plusDays(1));
        long day7Retained = calculateRetainedUsers(cohortUsers, cohortDate.plusDays(7));
        long day30Retained = calculateRetainedUsers(cohortUsers, cohortDate.plusDays(30));

        RetentionCohort cohort = retentionCohortRepository
            .findByCohortDate(cohortDate)
            .orElse(RetentionCohort.builder()
                .cohortDate(cohortDate)
                .build());

        cohort.setCohortSize(cohortSize);
        cohort.setDay1Count(day1Retained);
        cohort.setDay7Count(day7Retained);
        cohort.setDay30Count(day30Retained);

        if (cohortSize > 0) {
            cohort.setRetentionDay1((double) day1Retained / cohortSize * 100);
            cohort.setRetention7Day((double) day7Retained / cohortSize * 100);
            cohort.setRetention30Day((double) day30Retained / cohortSize * 100);
        }

        retentionCohortRepository.save(cohort);

        log.info("Retention cohort updated: date={}, size={}, day1={}%, day7={}%, day30={}%",
            cohortDate, cohortSize, cohort.getRetentionDay1(), cohort.getRetention7Day(), cohort.getRetention30Day());
    }

    /**
     * Calculate how many users from cohort were active on specific date
     */
    private long calculateRetainedUsers(List<UUID> cohortUsers, LocalDate checkDate) {
        LocalDateTime startOfDay = checkDate.atStartOfDay();
        LocalDateTime endOfDay = checkDate.plusDays(1).atStartOfDay();

        List<UUID> activeUsers = rawEventLogRepository
            .findUnprocessedByDateRange(startOfDay, endOfDay)
            .stream()
            .filter(e -> "SESSION_CREATED".equals(e.getEventType()))
            .map(RawEventLog::getUserId)
            .distinct()
            .collect(Collectors.toList());

        return cohortUsers.stream()
            .filter(activeUsers::contains)
            .count();
    }

    /**
     * Update device analytics from raw events
     */
    @Transactional
    public void updateDeviceAnalytics(LocalDate date) {
        log.info("Updating device analytics for date: {}", date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<RawEventLog> sessionEvents = rawEventLogRepository
            .findUnprocessedByDateRange(startOfDay, endOfDay)
            .stream()
            .filter(e -> "SESSION_CREATED".equals(e.getEventType()))
            .collect(Collectors.toList());

        Map<String, DeviceStats> deviceStatsMap = new HashMap<>();

        for (RawEventLog event : sessionEvents) {
            String deviceType = event.getPayload().path("deviceType").asText("UNKNOWN");
            UUID userId = event.getUserId();

            deviceStatsMap.putIfAbsent(deviceType, new DeviceStats());
            DeviceStats stats = deviceStatsMap.get(deviceType);
            stats.sessions++;
            stats.users.add(userId);
        }

        for (Map.Entry<String, DeviceStats> entry : deviceStatsMap.entrySet()) {
            String deviceType = entry.getKey();
            DeviceStats stats = entry.getValue();

            DeviceAnalytics analytics = deviceAnalyticsRepository
                .findByDateAndDeviceType(date, deviceType)
                .orElse(DeviceAnalytics.builder()
                    .date(date)
                    .deviceType(deviceType)
                    .build());

            analytics.setTotalSessions(stats.sessions);
            analytics.setUniqueUsers((long) stats.users.size());

            deviceAnalyticsRepository.save(analytics);

            log.info("Device analytics updated: date={}, device={}, sessions={}, users={}",
                date, deviceType, stats.sessions, stats.users.size());
        }
    }

    private static class DeviceStats {
        long sessions = 0;
        java.util.Set<UUID> users = new java.util.HashSet<>();
    }
}
