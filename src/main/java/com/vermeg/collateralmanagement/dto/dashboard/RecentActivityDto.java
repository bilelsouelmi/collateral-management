package com.vermeg.collateralmanagement.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDto {
    private String activityType; // PORTFOLIO_CREATED, ASSET_ADDED, ALERT_TRIGGERED, etc.
    private String description;
    private String entityType; // PORTFOLIO, ASSET, ALERT, MARGIN_CALL, REPORT
    private Long entityId;
    private String entityName;
    private String status;
    private String severity;
    private String icon; // For frontend display
    private String actionUrl; // For frontend navigation

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    private Long minutesAgo;
}