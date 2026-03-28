package iuh.fit.se.analyticsservice.dto.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyMetricsResponse {
    private LocalDate date;
    private Long newRegistrations;
    private Long dailyActiveUsers;
    private Long totalSessions;
    private Long successfulLogins;
    private Long failedLogins;
    private Double growthRate;
}
