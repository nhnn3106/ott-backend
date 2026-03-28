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
public class RetentionResponse {
    private LocalDate cohortDate;
    private Integer cohortSize;
    private Long day1Count;
    private Long day7Count;
    private Long day30Count;
    private Double retentionDay1;
    private Double retention7Day;
    private Double retention30Day;
}
