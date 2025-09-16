// ReportSearchRequest.java
package com.vermeg.collateralmanagement.dto.request;

import com.vermeg.collateralmanagement.enums.ReportType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for advanced report search
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSearchRequest {

    private String searchTerm;
    private String nameSearch;
    private String descriptionSearch;

    @Builder.Default
    private String searchMode = "CONTAINS";

    @Builder.Default
    private boolean caseSensitive = false;

    private List<ReportType> types;
    private List<ReportType> excludeTypes;
    private Boolean isGenerated;
    private List<String> statuses;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAfter;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdBefore;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedAfter;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedBefore;

    private String dateRange;

    private List<Long> userIds;
    private List<String> usernames;
    private List<String> departments;
    private Boolean ownedByCurrentUser;

    private List<Long> portfolioIds;
    private List<String> portfolioNames;
    private List<String> assetTypes;
    private List<String> currencies;

    private Long minFileSizeBytes;
    private Long maxFileSizeBytes;
    private Long minGenerationTimeSeconds;
    private Long maxGenerationTimeSeconds;

    private List<String> formats;
    private List<String> tags;
    private List<String> priorities;

    @Builder.Default
    private String sortBy = "createdAt";

    @Builder.Default
    private String sortDirection = "DESC";

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    private Integer maxResults;

    public boolean isValidDateRange() {
        if (createdAfter != null && createdBefore != null) {
            return !createdAfter.isAfter(createdBefore);
        }
        if (generatedAfter != null && generatedBefore != null) {
            return !generatedAfter.isAfter(generatedBefore);
        }
        return true;
    }

    public boolean isBasicSearch() {
        return searchTerm != null &&
                types == null &&
                userIds == null &&
                portfolioIds == null &&
                createdAfter == null &&
                createdBefore == null;
    }

    public int getActiveFilterCount() {
        int count = 0;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) count++;
        if (types != null && !types.isEmpty()) count++;
        if (statuses != null && !statuses.isEmpty()) count++;
        if (userIds != null && !userIds.isEmpty()) count++;
        if (portfolioIds != null && !portfolioIds.isEmpty()) count++;
        if (createdAfter != null || createdBefore != null) count++;
        if (tags != null && !tags.isEmpty()) count++;
        if (isGenerated != null) count++;
        return count;
    }
}