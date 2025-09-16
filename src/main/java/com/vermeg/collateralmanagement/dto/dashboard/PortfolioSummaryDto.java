package com.vermeg.collateralmanagement.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummaryDto {
    private BigDecimal totalValue;
    private BigDecimal totalMargin;
    private Integer totalPortfolios;
    private Integer totalAssets;
    private List<PortfolioBreakdownDto> portfolioBreakdown;
    private List<AssetAllocationDto> assetAllocation;
    private BigDecimal dailyChange;
    private Double dailyChangePercentage;
}