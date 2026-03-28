package iuh.fit.se.analyticsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * OpenFeign Client for User Service
 * 
 * Used to query User Service API when needed.
 * Note: Analytics should primarily use event sourcing from raw_events_log,
 * but this client is available for specific queries if needed.
 * 
 * @author OTT Platform Team
 */
@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}")
    Map<String, Object> getUserById(
        @PathVariable("userId") String userId,
        @RequestHeader("X-Internal-API-Key") String apiKey
    );

    @GetMapping("/api/users/stats")
    Map<String, Object> getUserStats(
        @RequestHeader("X-Internal-API-Key") String apiKey
    );
}
