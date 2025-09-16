package com.vermeg.collateralmanagement.dto.dashboard;

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
public class RiskMetricDto {
    private Long portfolioId;
    private String portfolioName;
    private BigDecimal currentRisk;
    private BigDecimal threshold;
    private String status; // NORMAL, WARNING, CRITICAL
    private String methodology;
    private LocalDateTime lastCalculated;
}