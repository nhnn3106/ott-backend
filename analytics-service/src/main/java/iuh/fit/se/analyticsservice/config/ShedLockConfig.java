package iuh.fit.se.analyticsservice.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * ShedLock Configuration
 * 
 * Provides distributed locking mechanism for scheduled jobs.
 * Prevents duplicate execution when multiple instances are running.
 * 
 * Features:
 * - Database-based locking (shedlock table)
 * - Uses database time (not application server time)
 * - Automatic lock release on job completion
 * - Lock expiration to handle crashed instances
 * 
 * @author OTT Platform Team
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {

    /**
     * Create lock provider using JDBC template
     * 
     * @param dataSource PostgreSQL datasource
     * @return LockProvider instance
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime() // Use database time for consistency across servers
                .build()
        );
    }
}
