// BulkReportResponse.java
package com.vermeg.collateralmanagement.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for bulk report generation operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkReportResponse {

    private String operationId;
    private int totalReports;
    private int successfulReports;
    private int failedReports;
    private int pendingReports;

    private String status;
    private int progressPercentage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime estimatedCompletionAt;

    private List<ReportResponse> reports;
    private List<String> errorMessages;
    private String zipDownloadUrl;

    public boolean isCompleted() {
        return "COMPLETED".equals(status) || "FAILED".equals(status);
    }

    public double getSuccessRate() {
        if (totalReports == 0) return 0.0;
        return (double) successfulReports / totalReports * 100.0;
    }

    public String getFormattedSuccessRate() {
        return String.format("%.1f%%", getSuccessRate());
    }
}