// AlertSearchRequest.java - Fixed return type issues
package com.vermeg.collateralmanagement.dto.request;

import com.vermeg.collateralmanagement.enums.AlertType;
import com.vermeg.collateralmanagement.enums.AlertSeverity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for advanced alert search
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertSearchRequest {

    private String searchTerm;
    private String titleSearch;
    private String messageSearch;

    @Builder.Default
    private String searchMode = "CONTAINS";

    @Builder.Default
    private boolean caseSensitive = false;

    private List<AlertType> types;
    private List<AlertType> excludeTypes;
    private List<AlertSeverity> severities;
    private Boolean isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAfter;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdBefore;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime triggeredAfter;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime triggeredBefore;

    private List<Long> userIds;
    private List<String> usernames;
    private Boolean ownedByCurrentUser;

    @Builder.Default
    private String sortBy = "createdAt";

    @Builder.Default
    private String sortDirection = "DESC";

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    private Integer maxResults;

    // FIXED: Added proper return types
    public boolean isValidDateRange() {
        if (createdAfter != null && createdBefore != null) {
            return !createdAfter.isAfter(createdBefore);
        }
        if (triggeredAfter != null && triggeredBefore != null) {
            return !triggeredAfter.isAfter(triggeredBefore);
        }
        return true;
    }

    public boolean isBasicSearch() {
        return searchTerm != null &&
                types == null &&
                userIds == null &&
                createdAfter == null &&
                createdBefore == null;
    }

    public int getActiveFilterCount() {
        int count = 0;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) count++;
        if (types != null && !types.isEmpty()) count++;
        if (severities != null && !severities.isEmpty()) count++;
        if (userIds != null && !userIds.isEmpty()) count++;
        if (createdAfter != null || createdBefore != null) count++;
        if (isRead != null) count++;
        return count;
    }
}