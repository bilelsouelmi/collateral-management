package com.vermeg.collateralmanagement.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsOverviewDto {
    private PerformanceMetricsDto performanceMetrics;
    private List<TrendDataDto> portfolioTrends;
    private List<TrendDataDto> riskTrends;
    private List<TrendDataDto> alertTrends;
    private LocalDateTime dataAsOf;
    private Integer periodDays;
}