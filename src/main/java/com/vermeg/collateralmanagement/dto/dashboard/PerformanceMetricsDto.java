package com.vermeg.collateralmanagement.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetricsDto {
    private BigDecimal totalReturn;
    private Double totalReturnPercentage;
    private Double volatility;
    private Double sharpeRatio;
    private Double maxDrawdown;
    private BigDecimal valueAtRisk;
    private String performanceRating; // EXCELLENT, GOOD, FAIR, POOR
}