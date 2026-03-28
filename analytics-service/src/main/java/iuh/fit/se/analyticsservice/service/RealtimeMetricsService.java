package iuh.fit.se.analyticsservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Realtime Metrics Service
 * 
 * Provides real-time analytics from Redis cache.
 * Updated by EventConsumerService on each event.
 * 
 * @author OTT Platform Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeMetricsService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Get current number of active users (last 1 hour)
     */
    public long getCurrentActiveUsers() {
        String key = "analytics:active:users";
        Set<Object> activeUsers = redisTemplate.opsForSet().members(key);
        return activeUsers != null ? activeUsers.size() : 0;
    }

    /**
     * Get today's total registrations
     */
    public long getTodayRegistrations() {
        String key = "analytics:today:registrations";
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value.toString()) : 0;
    }

    /**
     * Get today's successful logins
     */
    public long getTodaySuccessfulLogins() {
        String key = "analytics:today:logins:SUCCESS";
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value.toString()) : 0;
    }

    /**
     * Get today's failed logins
     */
    public long getTodayFailedLogins() {
        String key = "analytics:today:logins:FAILED";
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value.toString()) : 0;
    }

    /**
     * Get all today's metrics
     */
    public Map<String, Object> getTodayMetrics() {
        return Map.of(
            "activeUsers", getCurrentActiveUsers(),
            "newRegistrations", getTodayRegistrations(),
            "successfulLogins", getTodaySuccessfulLogins(),
            "failedLogins", getTodayFailedLogins()
        );
    }

    /**
     * Reset daily counters (called by scheduled job at midnight)
     */
    public void resetDailyCounters() {
        log.info("Resetting daily counters");
        redisTemplate.delete("analytics:today:registrations");
        redisTemplate.delete("analytics:today:logins:SUCCESS");
        redisTemplate.delete("analytics:today:logins:FAILED");
    }

    /**
     * Manually increment counter (for testing)
     */
    public void incrementCounter(String counterName, long delta) {
        String key = "analytics:today:" + counterName;
        redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * Add user to active users set
     */
    public void addActiveUser(String userId) {
        String key = "analytics:active:users";
        redisTemplate.opsForSet().add(key, userId);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
    }

    /**
     * Check cache health
     */
    public boolean isCacheHealthy() {
        try {
            String testKey = "analytics:health:test";
            redisTemplate.opsForValue().set(testKey, "OK", 10, TimeUnit.SECONDS);
            String result = (String) redisTemplate.opsForValue().get(testKey);
            return "OK".equals(result);
        } catch (Exception e) {
            log.error("Cache health check failed", e);
            return false;
        }
    }
}
