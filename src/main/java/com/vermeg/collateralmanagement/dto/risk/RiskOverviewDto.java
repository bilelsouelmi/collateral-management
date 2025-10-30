package com.vermeg.collateralmanagement.dto.risk;

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
public class RiskOverviewDto {
    private Integer totalPortfolios;
    private Integer highRiskPortfolios;
    private Integer mediumRiskPortfolios;
    private Integer lowRiskPortfolios;
    private BigDecimal totalValueAtRisk;
    private BigDecimal averageRiskScore;
    private String overallRiskRating; // LOW, MEDIUM, HIGH, CRITICAL
    private List<PortfolioRiskSummaryDto> portfolioRisks;
    private RiskDistributionDto riskDistribution;
    private LocalDateTime lastUpdated;
}