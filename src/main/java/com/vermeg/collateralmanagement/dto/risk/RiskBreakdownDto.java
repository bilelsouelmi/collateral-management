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
public class RiskBreakdownDto {
    private BigDecimal marketRisk;
    private BigDecimal creditRisk;
    private BigDecimal liquidityRisk;
    private BigDecimal concentrationRisk;
    private BigDecimal operationalRisk;
}