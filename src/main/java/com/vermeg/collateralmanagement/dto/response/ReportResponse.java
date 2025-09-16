// ReportResponse.java
package com.vermeg.collateralmanagement.dto.response;

import com.vermeg.collateralmanagement.enums.ReportType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for report information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {

    private Long id;
    private String name;
    private String description;
    private ReportType type;
    private String typeName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedAt;

    private boolean isGenerated;
    private boolean isRecent;
    private String status;
    private String fileName;
    private String downloadUrl;

    private Long userId;
    private String userName;
    private String userFullName;

    private String format;
    private Long fileSizeBytes;
    private Long generationTimeSeconds;
    private Integer progressPercentage;

    private Long portfolioId;
    private String portfolioName;
    private List<String> tags;

    @Builder.Default
    private Long downloadCount = 0L;
    private boolean containsSensitiveData;

    public static ReportResponse fromEntity(com.vermeg.collateralmanagement.entity.Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .name(report.getName())
                .description(report.getDescription())
                .type(report.getType())
                .typeName(report.getType().getDisplayName())
                .createdAt(report.getCreatedAt())
                .generatedAt(report.getGeneratedAt())
                .isGenerated(report.isGenerated())
                .isRecent(report.isRecent(24))
                .fileName(report.getFileName())
                .status(report.isGenerated() ? "GENERATED" : "PENDING")
                .userId(report.getUser().getId())
                .userName(report.getUser().getUsername())
                .userFullName(report.getUser().getFullName())
                .downloadUrl("/api/reports/" + report.getId() + "/download")
                .format("PDF")
                .build();
    }

    public String getFormattedFileSize() {
        if (fileSizeBytes == null) return "Unknown";

        double bytes = fileSizeBytes.doubleValue();
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;

        while (bytes >= 1024 && unitIndex < units.length - 1) {
            bytes /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", bytes, units[unitIndex]);
    }

    public boolean isDownloadable() {
        return isGenerated && "GENERATED".equals(status) && fileName != null;
    }
}