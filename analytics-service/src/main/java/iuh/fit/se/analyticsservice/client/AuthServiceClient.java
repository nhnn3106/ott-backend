package iuh.fit.se.analyticsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * OpenFeign Client for Auth Service
 * 
 * Used to query Auth Service API when needed.
 * Note: Analytics should primarily use event sourcing from raw_events_log,
 * but this client is available for specific queries if needed.
 * 
 * @author OTT Platform Team
 */
@FeignClient(name = "auth-service", url = "${services.auth-service.url}")
public interface AuthServiceClient {

    @GetMapping("/api/auth/stats")
    Map<String, Object> getAuthStats(
        @RequestHeader("X-Internal-API-Key") String apiKey
    );
}
