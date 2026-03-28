package iuh.fit.se.analyticsservice.controller;

import iuh.fit.se.analyticsservice.dto.response.ApiResponse;
import iuh.fit.se.analyticsservice.service.MetricsAggregationService;
import iuh.fit.se.analyticsservice.service.RealtimeMetricsService;
import iuh.fit.se.analyticsservice.service.UserAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Admin Controller
 * 
 * Admin-only endpoints for manual operations and system management.
 * Requires ROLE_ADMIN.
 * 
 * @author OTT Platform Team
 */
@RestController
@RequestMapping("/api/analytics/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final MetricsAggregationService metricsAggregationService;
    private final RealtimeMetricsService realtimeMetricsService;
    private final UserAnalyticsService userAnalyticsService;

    /**
     * POST /api/analytics/admin/calculate-daily
     * Manually trigger daily metrics calculation
     */
    @PostMapping("/calculate-daily")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateDailyMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("Admin triggered daily metrics calculation for {}", date);
        metricsAggregationService.calculateDailyMetrics(date);
        return ResponseEntity.ok(ApiResponse.success("Daily metrics calculated", Map.of("date", date)));
    }

    /**
     * POST /api/analytics/admin/calculate-monthly
     * Manually trigger monthly metrics calculation
     */
    @PostMapping("/calculate-monthly")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateMonthlyMetrics(
            @RequestParam int year,
            @RequestParam int month) {
        
        log.info("Admin triggered monthly metrics calculation for {}-{}", year, month);
        metricsAggregationService.calculateMonthlyMetrics(year, month);
        return ResponseEntity.ok(ApiResponse.success("Monthly metrics calculated", Map.of("year", year, "month", month)));
    }

    /**
     * POST /api/analytics/admin/update-retention
     * Manually trigger retention cohort update
     */
    @PostMapping("/update-retention")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRetention(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cohortDate) {
        
        log.info("Admin triggered retention update for {}", cohortDate);
        metricsAggregationService.updateRetentionCohorts(cohortDate);
        return ResponseEntity.ok(ApiResponse.success("Retention updated", Map.of("cohortDate", cohortDate)));
    }

    /**
     * POST /api/analytics/admin/update-devices
     * Manually trigger device analytics update
     */
    @PostMapping("/update-devices")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateDeviceAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("Admin triggered device analytics update for {}", date);
        metricsAggregationService.updateDeviceAnalytics(date);
        return ResponseEntity.ok(ApiResponse.success("Device analytics updated", Map.of("date", date)));
    }

    /**
     * POST /api/analytics/admin/reset-counters
     * Manually reset daily counters
     */
    @PostMapping("/reset-counters")
    public ResponseEntity<ApiResponse<Void>> resetCounters() {
        log.warn("Admin manually reset daily counters");
        realtimeMetricsService.resetDailyCounters();
        return ResponseEntity.ok(ApiResponse.success("Counters reset", null));
    }

    /**
     * GET /api/analytics/admin/health
     * Get system health status
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealth() {
        log.info("Admin checking system health");
        Map<String, Object> health = userAnalyticsService.getHealthStatus();
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    /**
     * POST /api/analytics/admin/cache/increment
     * Manually increment a counter (for testing)
     */
    @PostMapping("/cache/increment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> incrementCounter(
            @RequestParam String counterName,
            @RequestParam(defaultValue = "1") long delta) {
        
        log.info("Admin incrementing counter {} by {}", counterName, delta);
        realtimeMetricsService.incrementCounter(counterName, delta);
        return ResponseEntity.ok(ApiResponse.success("Counter incremented", Map.of("counter", counterName, "delta", delta)));
    }
}
