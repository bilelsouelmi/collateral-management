// ReportSystemHealth.java
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
 * Response DTO for report system health monitoring
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSystemHealth {

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastChecked;

    private double healthScore;
    private double uptimePercentage;

    private long pendingReports;
    private long staleReports;
    private long slowReports;
    private long reportsInProgress;
    private long estimatedQueueTime;

    private double systemLoad;
    private Double averageGenerationTimeMinutes;
    private Double generationSuccessRate;
    private String peakPerformanceHour;
    private double cpuUsage;
    private double memoryUsage;

    private Long totalStorageUsedMB;
    private Long availableStorageMB;
    private double storageUsagePercentage;
    private long reportFilesCount;

    private Integer activeUsers;
    private Integer concurrentGenerations;
    private Integer peakConcurrentUsers;

    private List<String> issues;
    private List<String> recommendations;
    private List<SystemAlert> recentAlerts;

    private Map<String, ComponentStatus> componentStatus;
    private String databaseStatus;
    private String fileSystemStatus;
    private String externalServicesStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSystemRestart;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMaintenanceDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextMaintenanceWindow;

    private boolean maintenanceMode;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemAlert {
        private String severity;
        private String message;
        private LocalDateTime timestamp;
        private String component;
        private boolean acknowledged;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentStatus {
        private String name;
        private String status;
        private String version;
        private LocalDateTime lastChecked;
        private String errorMessage;
        private double responseTime;
    }

    public boolean isHealthy() {
        return "HEALTHY".equals(status);
    }

    public boolean hasWarnings() {
        return "WARNING".equals(status);
    }

    public boolean isCritical() {
        return "CRITICAL".equals(status);
    }

    public long getCriticalIssuesCount() {
        if (issues == null) return 0;
        return issues.stream()
                .mapToLong(issue -> issue.toLowerCase().contains("critical") ? 1 : 0)
                .sum();
    }

    public boolean isStorageLow() {
        return storageUsagePercentage > 80.0;
    }

    public boolean isHighLoad() {
        return systemLoad > 90.0;
    }

    public String getFormattedStorageUsage() {
        if (totalStorageUsedMB == null) return "N/A";
        if (totalStorageUsedMB < 1024) {
            return totalStorageUsedMB + " MB";
        } else {
            double gb = totalStorageUsedMB / 1024.0;
            return String.format("%.1f GB", gb);
        }
    }

    public String getHealthSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("System Status: ").append(status);
        summary.append(", Health Score: ").append(String.format("%.0f%%", healthScore));
        summary.append(", Pending Reports: ").append(pendingReports);
        summary.append(", System Load: ").append(String.format("%.1f%%", systemLoad));

        if (issues != null && !issues.isEmpty()) {
            summary.append(", Issues: ").append(issues.size());
        }

        return summary.toString();
    }

    public boolean requiresImmediateAction() {
        return isCritical() ||
                staleReports > 10 ||
                systemLoad > 95.0 ||
                storageUsagePercentage > 95.0 ||
                (generationSuccessRate != null && generationSuccessRate < 80.0);
    }
}