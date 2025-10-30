package com.vermeg.collateralmanagement.dto.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetRiskDto {
    private Long assetId;
    private String assetName;
    private String assetType;
    private BigDecimal currentValue;
    private BigDecimal percentOfPortfolio;
    private BigDecimal volatility;
    private BigDecimal beta;
    private BigDecimal valueAtRisk;
    private BigDecimal riskContribution;
    private String riskRating;
}