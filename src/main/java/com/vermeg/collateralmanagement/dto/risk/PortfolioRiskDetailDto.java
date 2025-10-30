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
public class PortfolioRiskDetailDto {
    private Long portfolioId;
    private String portfolioName;
    private String portfolioType;
    private BigDecimal totalValue;
    private BigDecimal riskScore;
    private String riskRating;
    private BigDecimal valueAtRisk;
    private BigDecimal expectedShortfall;
    private BigDecimal sharpeRatio;
    private BigDecimal beta;
    private BigDecimal volatility;
    private BigDecimal maxDrawdown;
    private List<AssetRiskDto> assetRisks;
    private RiskBreakdownDto riskBreakdown;
    private List<HistoricalPerformanceDto> historicalPerformance;
    private String methodology;
    private LocalDateTime lastUpdated;
}