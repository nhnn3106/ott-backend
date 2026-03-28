package iuh.fit.se.analyticsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

/**
 * Analytics Service Application
 * 
 * Provides user analytics and metrics for OTT Platform admin dashboard.
 * Features:
 * - User growth tracking (DAU, MAU)
 * - Retention cohort analysis
 * - Device and geographic analytics
 * - Login method statistics
 * - Real-time active users monitoring
 * 
 * Architecture:
 * - Event-driven: Consumes events from RabbitMQ
 * - Event sourcing: Stores raw events for batch processing
 * - Hybrid analytics: Real-time (Redis) + Batch (PostgreSQL)
 * - Distributed scheduling: ShedLock prevents duplicate job execution
 * 
 * @author OTT Platform Team
 * @version 1.0
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@EnableFeignClients
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
