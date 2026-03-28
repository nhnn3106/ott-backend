package iuh.fit.se.analyticsservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyMetricsResponse {
    private Integer year;
    private Integer month;
    private Long totalRegistrations;
    private Long monthlyActiveUsers;
    private Long averageDailyActiveUsers;
    private Long totalSessions;
    private Long totalLogins;
}
