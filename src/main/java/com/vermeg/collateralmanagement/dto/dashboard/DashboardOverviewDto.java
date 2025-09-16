package com.vermeg.collateralmanagement.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDto {
    private PortfolioSummaryDto portfolioSummary;
    private RiskSummaryDto riskSummary;
    private AlertSummaryDto alertSummary;
    private MarginCallSummaryDto marginCallSummary;
    private List<RecentActivityDto> recentActivities;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdated;
}