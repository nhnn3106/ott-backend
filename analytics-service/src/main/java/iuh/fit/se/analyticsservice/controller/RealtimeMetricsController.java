package iuh.fit.se.analyticsservice.controller;

import iuh.fit.se.analyticsservice.dto.response.ApiResponse;
import iuh.fit.se.analyticsservice.service.RealtimeMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Realtime Metrics Controller
 * 
 * REST API for real-time analytics from Redis cache.
 * 
 * @author OTT Platform Team
 */
@RestController
@RequestMapping("/api/analytics/realtime")
@RequiredArgsConstructor
@Slf4j
public class RealtimeMetricsController {

    private final RealtimeMetricsService realtimeMetricsService;

    /**
     * GET /api/analytics/realtime/active-users
     * Get current active users (last 1 hour)
     */
    @GetMapping("/active-users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentActiveUsers() {
        log.info("Getting current active users");
        long activeUsers = realtimeMetricsService.getCurrentActiveUsers();
        return ResponseEntity.ok(ApiResponse.success(Map.of("activeUsers", activeUsers)));
    }

    /**
     * GET /api/analytics/realtime/today
     * Get today's metrics
     */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodayMetrics() {
        log.info("Getting today's metrics");
        Map<String, Object> metrics = realtimeMetricsService.getTodayMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    /**
     * GET /api/analytics/realtime/registrations
     * Get today's registrations
     */
    @GetMapping("/registrations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodayRegistrations() {
        log.info("Getting today's registrations");
        long registrations = realtimeMetricsService.getTodayRegistrations();
        return ResponseEntity.ok(ApiResponse.success(Map.of("registrations", registrations)));
    }

    /**
     * GET /api/analytics/realtime/logins
     * Get today's login statistics
     */
    @GetMapping("/logins")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodayLogins() {
        log.info("Getting today's logins");
        long successful = realtimeMetricsService.getTodaySuccessfulLogins();
        long failed = realtimeMetricsService.getTodayFailedLogins();
        
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "successful", successful,
            "failed", failed,
            "total", successful + failed
        )));
    }
}
