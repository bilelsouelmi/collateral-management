package com.vermeg.collateralmanagement.dto.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioRiskSummaryDto {
    private Long portfolioId;
    private String portfolioName;
    private String portfolioType;
    private BigDecimal totalValue;
    private BigDecimal riskScore;
    private String riskRating; // LOW, MEDIUM, HIGH, CRITICAL
    private BigDecimal valueAtRisk;
    private BigDecimal volatility;
    private BigDecimal beta;
    private LocalDateTime lastCalculated;
}