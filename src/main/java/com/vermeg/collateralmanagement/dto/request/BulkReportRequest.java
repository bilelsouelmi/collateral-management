// BulkReportRequest.java
package com.vermeg.collateralmanagement.dto.request;

import com.vermeg.collateralmanagement.enums.ReportType;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for bulk report generation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkReportRequest {

    @NotEmpty(message = "At least one report type must be specified")
    private List<ReportType> reportTypes;

    private String namePrefix;

    @Builder.Default
    private String description = "Bulk generated reports";

    private List<Long> portfolioIds;
    private List<Long> userIds;

    @Builder.Default
    private String format = "PDF";

    @Builder.Default
    private boolean generateImmediately = true;

    @Builder.Default
    private Integer maxConcurrentGenerations = 3;

    @Builder.Default
    private String priority = "MEDIUM";

    private Map<ReportType, Map<String, Object>> typeSpecificParameters;
    private Map<ReportType, String> customNamesByType;

    @Builder.Default
    private boolean createZipPackage = false;
    private String zipPackageName;

    @Builder.Default
    private boolean emailOnCompletion = false;
    private String emailRecipients;

    @Builder.Default
    private boolean continueOnError = true;

    @Builder.Default
    private Integer maxFailures = 3;

    private List<String> tags;
    private String businessJustification;

    public int getTotalReportCount() {
        if (reportTypes == null) return 0;

        int typeCount = reportTypes.size();
        int scopeMultiplier = 1;

        if (portfolioIds != null && !portfolioIds.isEmpty()) {
            scopeMultiplier = portfolioIds.size();
        }

        if (userIds != null && !userIds.isEmpty()) {
            scopeMultiplier = Math.max(scopeMultiplier, userIds.size());
        }

        return typeCount * scopeMultiplier;
    }

    public String getCustomNameForType(ReportType type) {
        if (customNamesByType != null && customNamesByType.containsKey(type)) {
            return customNamesByType.get(type);
        }

        String prefix = namePrefix != null ? namePrefix + "_" : "";
        return prefix + type.getDisplayName();
    }
}