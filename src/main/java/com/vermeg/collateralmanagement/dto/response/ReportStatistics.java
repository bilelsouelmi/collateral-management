// ReportStatistics.java
package com.vermeg.collateralmanagement.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for report generation statistics and analytics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportStatistics {

    private long totalReports;
    private long generatedReports;
    private long pendingReports;
    private long recentReports;

    private Map<String, Long> reportsByType;
    private Map<String, Long> reportsByUser;
    private Map<String, Long> reportsByStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastGeneratedAt;

    private long reportsGeneratedToday;
    private long reportsGeneratedThisWeek;
    private long reportsGeneratedThisMonth;

    private double generationRate;
    private Double averageGenerationTimeMinutes;
    private String peakGenerationHour;

    private long totalUsersWithReports;
    private double averageReportsPerUser;
    private String mostActiveUser;
    private long activeUsersLast30Days;

    private String mostPopularReportType;
    private double systemLoadPercentage;
    private long storageUsageMB;
    private long failedReports;

    private double growthRate;
    private String trendDirection;
    private String performanceTrend;

    public double getSuccessRate() {
        if (totalReports == 0) return 0.0;
        return (double) generatedReports / totalReports * 100;
    }

    public double getFailureRate() {
        return 100.0 - getSuccessRate();
    }

    public boolean isHealthy() {
        return getSuccessRate() >= 95.0 &&
                pendingReports < 10 &&
                systemLoadPercentage < 80.0;
    }

    public String getSystemStatus() {
        if (getSuccessRate() < 85.0 || pendingReports > 20 || systemLoadPercentage > 90.0) {
            return "CRITICAL";
        } else if (getSuccessRate() < 95.0 || pendingReports > 10 || systemLoadPercentage > 80.0) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }

    public String getFormattedGenerationRate() {
        return String.format("%.1f%%", generationRate);
    }

    public String getFormattedAverageGenerationTime() {
        if (averageGenerationTimeMinutes == null) {
            return "N/A";
        }
        if (averageGenerationTimeMinutes < 1.0) {
            return String.format("%.0f seconds", averageGenerationTimeMinutes * 60);
        }
        return String.format("%.1f minutes", averageGenerationTimeMinutes);
    }
}