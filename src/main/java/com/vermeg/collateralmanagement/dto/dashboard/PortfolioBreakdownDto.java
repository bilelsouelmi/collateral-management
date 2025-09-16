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
public class PortfolioBreakdownDto {
    private Long portfolioId;
    private String portfolioName;
    private String portfolioType;
    private BigDecimal totalValue;
    private BigDecimal totalMargin;
    private Integer assetCount;
    private String status;
}
