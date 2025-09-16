package com.vermeg.collateralmanagement.dto.analytics;

import com.vermeg.collateralmanagement.dto.dashboard.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryDto {
    private PortfolioSummaryDto portfolioSummary;
    private RiskSummaryDto riskSummary;
    private AlertSummaryDto alertSummary;
    private Integer periodDays;
    private LocalDateTime generatedAt;
}