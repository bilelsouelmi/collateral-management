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
public class RiskSummaryDto {
    private BigDecimal overallRiskScore;
    private String riskRating; // LOW, MEDIUM, HIGH, CRITICAL
    private BigDecimal valueAtRisk;
    private Double utilizationPercentage;
    private Integer portfoliosAtRisk;
    private List<RiskMetricDto> riskMetrics;
    private List<RiskAlertDto> riskAlerts;
    private LocalDateTime lastCalculated;
}
