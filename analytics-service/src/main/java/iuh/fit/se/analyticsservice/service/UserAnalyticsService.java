package iuh.fit.se.analyticsservice.service;

import iuh.fit.se.analyticsservice.entity.*;
import iuh.fit.se.analyticsservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User Analytics Service
 * 
 * Main business logic for analytics queries.
 * Provides high-level analytics methods for REST API controllers.
 * 
 * @author OTT Platform Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAnalyticsService {

    private final DailyUserMetricsRepository dailyUserMetricsRepository;
    private final MonthlyUserMetricsRepository monthlyUserMetricsRepository;
    private final RetentionCohortRepository retentionCohortRepository;
    private final DeviceAnalyticsRepository deviceAnalyticsRepository;
    private final GeographicAnalyticsRepository geographicAnalyticsRepository;
    private final LoginMethodAnalyticsRepository loginMethodAnalyticsRepository;
    private final RealtimeMetricsService realtimeMetricsService;

    /**
     * Get user growth metrics for specified period
     */
    @Cacheable(value = "userGrowth", key = "#startDate + '-' + #endDate")
    public Map<String, Object> getUserGrowth(LocalDate startDate, LocalDate endDate) {
        log.info("Getting user growth: {} to {}", startDate, endDate);

        List<DailyUserMetrics> dailyMetrics = dailyUserMetricsRepository
            .findByDateBetweenOrderByDateDesc(startDate, endDate);

        long totalRegistrations = dailyMetrics.stream()
            .mapToLong(DailyUserMetrics::getNewRegistrations)
            .sum();

        double averageGrowthRate = dailyMetrics.stream()
            .filter(m -> m.getGrowthRate() != null)
            .mapToDouble(DailyUserMetrics::getGrowthRate)
            .average()
            .orElse(0.0);

        return Map.of(
            "period", Map.of("start", startDate, "end", endDate),
            "totalRegistrations", totalRegistrations,
            "averageGrowthRate", averageGrowthRate,
            "dailyMetrics", dailyMetrics
        );
    }

    /**
     * Get active users statistics
     */
    @Cacheable(value = "activeUsers", key = "#startDate + '-' + #endDate")
    public Map<String, Object> getActiveUsers(LocalDate startDate, LocalDate endDate) {
        log.info("Getting active users: {} to {}", startDate, endDate);

        List<DailyUserMetrics> dailyMetrics = dailyUserMetricsRepository
            .findByDateBetweenOrderByDateDesc(startDate, endDate);

        Double avgDau = dailyUserMetricsRepository
            .avgDailyActiveUsersByDateRange(startDate, endDate);

        // Get MAU from most recent month
        LocalDate now = LocalDate.now();
        MonthlyUserMetrics currentMonth = monthlyUserMetricsRepository
            .findByYearAndMonth(now.getYear(), now.getMonthValue())
            .orElse(null);

        // Get current active users from Redis
        long currentActive = realtimeMetricsService.getCurrentActiveUsers();

        return Map.of(
            "period", Map.of("start", startDate, "end", endDate),
            "averageDailyActiveUsers", avgDau != null ? avgDau : 0,
            "monthlyActiveUsers", currentMonth != null ? currentMonth.getMonthlyActiveUsers() : 0,
            "currentActiveUsers", currentActive,
            "dailyMetrics", dailyMetrics
        );
    }

    /**
     * Get retention analysis
     */
    @Cacheable(value = "retention", key = "#startDate + '-' + #endDate")
    public Map<String, Object> getRetentionAnalysis(LocalDate startDate, LocalDate endDate) {
        log.info("Getting retention analysis: {} to {}", startDate, endDate);

        List<RetentionCohort> cohorts = retentionCohortRepository
            .findByCohortDateBetweenOrderByCohortDateDesc(startDate, endDate);

        Double avgRetention7Day = retentionCohortRepository
            .avgRetention7DayByDateRange(startDate, endDate);

        Double avgRetention30Day = retentionCohortRepository
            .avgRetention30DayByDateRange(startDate, endDate);

        return Map.of(
            "period", Map.of("start", startDate, "end", endDate),
            "averageRetention7Day", avgRetention7Day != null ? avgRetention7Day : 0,
            "averageRetention30Day", avgRetention30Day != null ? avgRetention30Day : 0,
            "cohorts", cohorts
        );
    }

    /**
     * Get device distribution
     */
    @Cacheable(value = "deviceDistribution", key = "#startDate + '-' + #endDate")
    public Map<String, Object> getDeviceDistribution(LocalDate startDate, LocalDate endDate) {
        log.info("Getting device distribution: {} to {}", startDate, endDate);

        List<Object[]> deviceStats = deviceAnalyticsRepository
            .sumSessionsByDeviceType(startDate, endDate);

        Map<String, Long> distribution = new HashMap<>();
        long totalSessions = 0;

        for (Object[] row : deviceStats) {
            String deviceType = (String) row[0];
            Long sessions = ((Number) row[1]).longValue();
            distribution.put(deviceType, sessions);
            totalSessions += sessions;
        }

        return Map.of(
            "period", Map.of("start", startDate, "end", endDate),
            "totalSessions", totalSessions,
            "distribution", distribution
        );
    }

    /**
     * Get geographic distribution
     */
    @Cacheable(value = "geoDistribution", key = "#startDate + '-' + #endDate")
    public Map<String, Object> getGeographicDistribution(LocalDate startDate, LocalDate endDate) {
        log.info("Getting geographic distribution: {} to {}", startDate, endDate);

        List<Object[]> countryStats = geographicAnalyticsRepository
            .sumUsersByCountry(startDate, endDate);

        Map<String, Long> distribution = new HashMap<>();
        long totalUsers = 0;

        for (Object[] row : countryStats) {
            String country = (String) row[0];
            Long users = ((Number) row[1]).longValue();
            distribution.put(country, users);
            totalUsers += users;
        }

        return Map.of(
            "period", Map.of("start", startDate, "end", endDate),
            "totalUsers", totalUsers,
            "distribution", distribution
        );
    }

    /**
     * Get login method statistics
     */
    @Cacheable(value = "loginMethods", key = "#startDate + '-' + #endDate")
    public Map<String, Object> getLoginMethodStats(LocalDate startDate, LocalDate endDate) {
        log.info("Getting login method stats: {} to {}", startDate, endDate);

        List<Object[]> methodStats = loginMethodAnalyticsRepository
            .sumSuccessfulAttemptsByMethod(startDate, endDate);

        Map<String, Long> distribution = new HashMap<>();
        long totalAttempts = 0;

        for (Object[] row : methodStats) {
            String method = (String) row[0];
            Long attempts = ((Number) row[1]).longValue();
            distribution.put(method, attempts);
            totalAttempts += attempts;
        }

        return Map.of(
            "period", Map.of("start", startDate, "end", endDate),
            "totalAttempts", totalAttempts,
            "distribution", distribution
        );
    }

    /**
     * Get comprehensive dashboard summary
     */
    @Cacheable(value = "dashboardSummary", key = "#days")
    public Map<String, Object> getDashboardSummary(int days) {
        log.info("Getting dashboard summary for last {} days", days);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        // Get today's realtime metrics
        Map<String, Object> todayMetrics = realtimeMetricsService.getTodayMetrics();

        // Get recent daily metrics
        List<DailyUserMetrics> recentMetrics = dailyUserMetricsRepository
            .findRecentMetrics(startDate);

        // Calculate totals
        long totalRegistrations = recentMetrics.stream()
            .mapToLong(DailyUserMetrics::getNewRegistrations)
            .sum();

        long totalSessions = recentMetrics.stream()
            .mapToLong(DailyUserMetrics::getTotalSessions)
            .sum();

        // Get retention
        Double avgRetention7Day = retentionCohortRepository
            .avgRetention7DayByDateRange(startDate, endDate);

        return Map.of(
            "period", Map.of(
                "start", startDate,
                "end", endDate,
                "days", days
            ),
            "today", todayMetrics,
            "summary", Map.of(
                "totalRegistrations", totalRegistrations,
                "totalSessions", totalSessions,
                "averageRetention7Day", avgRetention7Day != null ? avgRetention7Day : 0
            ),
            "recentMetrics", recentMetrics
        );
    }

    /**
     * Get analytics health status
     */
    public Map<String, Object> getHealthStatus() {
        long pendingEvents = dailyUserMetricsRepository.count();
        boolean cacheHealthy = realtimeMetricsService.isCacheHealthy();

        return Map.of(
            "status", cacheHealthy ? "HEALTHY" : "DEGRADED",
            "cacheHealthy", cacheHealthy,
            "totalMetrics", pendingEvents,
            "timestamp", System.currentTimeMillis()
        );
    }
}
