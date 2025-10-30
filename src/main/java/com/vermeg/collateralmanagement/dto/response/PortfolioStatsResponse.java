// src/main/java/com/vermeg/collateralmanagement/dto/response/PortfolioStatsResponse.java
package com.vermeg.collateralmanagement.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioStatsResponse {
    private Long portfolioId;
    private Integer totalAssets;
    private BigDecimal totalValue;
    private BigDecimal totalMargin;
    private Integer activeAssets;
}