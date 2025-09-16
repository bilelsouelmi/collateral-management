package com.vermeg.collateralmanagement.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSummaryDto {
    private Integer totalAlerts;
    private Integer criticalAlerts;
    private Integer highAlerts;
    private Integer mediumAlerts;
    private Integer lowAlerts;
    private Integer unreadAlerts;
    private List<AlertOverviewDto> recentAlerts;
    private AlertTrendsDto alertTrends;
}