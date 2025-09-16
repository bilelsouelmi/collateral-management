package com.vermeg.collateralmanagement.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertTrendsDto {
    private List<AlertTrendDataDto> dailyTrends;
    private Double averageAlertsPerDay;
    private Double resolutionRate;
    private Long averageResolutionTimeMinutes;
}