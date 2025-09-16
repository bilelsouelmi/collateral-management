package com.vermeg.collateralmanagement.dto.response;

import com.vermeg.collateralmanagement.enums.AlertType;
import com.vermeg.collateralmanagement.enums.AlertSeverity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Response DTO for alert information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertResponse {

    private Long id;
    private String title;
    private String message;
    private AlertType type;
    private String typeName;
    private AlertSeverity severity;
    private String severityName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime triggeredAt;

    private boolean isRead;
    private boolean isUnread;
    private boolean isCritical;
    private boolean requiresImmediateAttention;

    private Long userId;
    private String userName;
    private String userFullName;

    private long minutesSinceTriggered;
    private String timeAgo;

    public static AlertResponse fromEntity(com.vermeg.collateralmanagement.entity.Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .type(alert.getType())
                .typeName(alert.getType().getDisplayName())
                .severity(alert.getSeverity())
                .severityName(alert.getSeverity().getDisplayName())
                .createdAt(alert.getCreatedAt())
                .triggeredAt(alert.getTriggeredAt())
                .isRead(alert.getIsRead())
                .isUnread(alert.isUnread())
                .isCritical(alert.isCritical())
                .requiresImmediateAttention(alert.requiresImmediateAttention())
                .userId(alert.getUser().getId())
                .userName(alert.getUser().getUsername())
                .userFullName(alert.getUser().getFullName())
                .minutesSinceTriggered(alert.getMinutesSinceTriggered())
                .timeAgo(formatTimeAgo(alert.getMinutesSinceTriggered()))
                .build();
    }

    private static String formatTimeAgo(long minutes) {
        if (minutes < 60) {
            return minutes + " minutes ago";
        } else if (minutes < 1440) { // 24 hours
            return (minutes / 60) + " hours ago";
        } else {
            return (minutes / 1440) + " days ago";
        }
    }

    public String getDisplaySummary() {
        return String.format("[%s] %s - %s",
                severity.getDisplayName(),
                type.getDisplayName(),
                title);
    }
}