// AlertDashboardResponse.java - Fixed variable naming conflict
package com.vermeg.collateralmanagement.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for alert dashboard summary
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDashboardResponse {

    // Summary counts
    private long totalAlerts;
    private long unreadAlerts;
    private long criticalAlertsCount;  // FIXED: Renamed from criticalAlerts to criticalAlertsCount
    private long highPriorityAlerts;
    private long overdueAlerts;

    // Recent alerts (last 5-10)
    private List<AlertResponse> recentAlerts;

    // Critical alerts requiring immediate attention
    private List<AlertResponse> criticalAlertsList;  // FIXED: Renamed from criticalAlerts to criticalAlertsList

    // Distribution charts data
    private Map<String, Long> alertsByType;
    private Map<String, Long> alertsBySeverity;
    private Map<String, Long> alertsByStatus;

    // Trend data (last 7 days)
    private List<DailyAlertCount> alertTrend;

    // Performance metrics
    private double averageResponseTimeHours;
    private double alertResolutionRate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastRefreshed;

    // System health indicators
    private String systemHealthStatus;
    private List<String> systemHealthIssues;
    private List<String> systemRecommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyAlertCount {
        private String date;
        private long totalAlerts;
        private long criticalAlerts;
        private long resolvedAlerts;
    }

    public boolean needsImmediateAttention() {
        return criticalAlertsCount > 0 || overdueAlerts > 0;
    }

    public String getHealthSummary() {
        if (criticalAlertsCount > 5) {
            return "CRITICAL - High number of critical alerts";
        } else if (overdueAlerts > 0) {
            return "WARNING - Overdue alerts detected";
        } else if (unreadAlerts > 20) {
            return "ATTENTION - Many unread alerts";
        } else {
            return "HEALTHY - System operating normally";
        }
    }
}