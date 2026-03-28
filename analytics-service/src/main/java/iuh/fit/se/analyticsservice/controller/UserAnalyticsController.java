package iuh.fit.se.analyticsservice.controller;

import iuh.fit.se.analyticsservice.dto.response.ApiResponse;
import iuh.fit.se.analyticsservice.service.UserAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * User Analytics Controller
 * 
 * REST API for user analytics queries.
 * Accessible by authenticated users and admins.
 * 
 * @author OTT Platform Team
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class UserAnalyticsController {

    private final UserAnalyticsService userAnalyticsService;

    /**
     * GET /api/analytics/growth
     * Get user growth metrics
     */
    @GetMapping("/growth")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserGrowth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Getting user growth: {} to {}", startDate, endDate);
        Map<String, Object> data = userAnalyticsService.getUserGrowth(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * GET /api/analytics/active-users
     * Get active users statistics
     */
    @GetMapping("/active-users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveUsers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Getting active users: {} to {}", startDate, endDate);
        Map<String, Object> data = userAnalyticsService.getActiveUsers(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * GET /api/analytics/retention
     * Get retention analysis
     */
    @GetMapping("/retention")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRetention(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Getting retention: {} to {}", startDate, endDate);
        Map<String, Object> data = userAnalyticsService.getRetentionAnalysis(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * GET /api/analytics/devices
     * Get device distribution
     */
    @GetMapping("/devices")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeviceDistribution(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Getting device distribution: {} to {}", startDate, endDate);
        Map<String, Object> data = userAnalyticsService.getDeviceDistribution(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * GET /api/analytics/geography
     * Get geographic distribution
     */
    @GetMapping("/geography")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGeography(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Getting geographic distribution: {} to {}", startDate, endDate);
        Map<String, Object> data = userAnalyticsService.getGeographicDistribution(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * GET /api/analytics/login-methods
     * Get login method statistics
     */
    @GetMapping("/login-methods")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLoginMethods(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Getting login methods: {} to {}", startDate, endDate);
        Map<String, Object> data = userAnalyticsService.getLoginMethodStats(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * GET /api/analytics/dashboard
     * Get comprehensive dashboard summary
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(
            @RequestParam(defaultValue = "30") int days) {
        
        log.info("Getting dashboard summary for {} days", days);
        Map<String, Object> data = userAnalyticsService.getDashboardSummary(days);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
