package com.vermeg.collateralmanagement.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Response DTO for bulk alert operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAlertResponse {

    private String operationId;
    private String operation;
    private int totalAlerts;
    private int successfulOperations;
    private int failedOperations;
    private int skippedOperations;

    private String status; // PENDING, IN_PROGRESS, COMPLETED, FAILED, COMPLETED_WITH_ERRORS
    private int progressPercentage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime estimatedCompletionAt;

    @Builder.Default
    private List<Long> processedAlertIds = new ArrayList<>();
    @Builder.Default
    private List<Long> failedAlertIds = new ArrayList<>();
    @Builder.Default
    private List<Long> skippedAlertIds = new ArrayList<>();
    @Builder.Default
    private List<String> errorMessages = new ArrayList<>();
    @Builder.Default
    private List<String> warningMessages = new ArrayList<>();

    private String operationSummary;
    private String operatorUsername;
    private String reason;

    public boolean isCompleted() {
        return "COMPLETED".equals(status) || "FAILED".equals(status) || "COMPLETED_WITH_ERRORS".equals(status);
    }

    public boolean isSuccessful() {
        return "COMPLETED".equals(status);
    }

    public boolean hasErrors() {
        return failedOperations > 0 || !errorMessages.isEmpty();
    }

    public boolean hasWarnings() {
        return skippedOperations > 0 || !warningMessages.isEmpty();
    }

    public double getSuccessRate() {
        if (totalAlerts == 0) return 0.0;
        return (double) successfulOperations / totalAlerts * 100.0;
    }

    public String getFormattedSuccessRate() {
        return String.format("%.1f%%", getSuccessRate());
    }

    public String getDetailedSummary() {
        return String.format(
                "Operation '%s': %d total, %d successful, %d failed, %d skipped (%.1f%% success rate)",
                operation, totalAlerts, successfulOperations, failedOperations,
                skippedOperations, getSuccessRate()
        );
    }

    public long getElapsedTimeMillis() {
        if (startedAt == null) return 0;
        LocalDateTime endTime = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, endTime).toMillis();
    }

    public String getFormattedElapsedTime() {
        long millis = getElapsedTimeMillis();
        if (millis < 1000) {
            return millis + "ms";
        } else if (millis < 60000) {
            return String.format("%.1fs", millis / 1000.0);
        } else {
            return String.format("%.1fm", millis / 60000.0);
        }
    }
}