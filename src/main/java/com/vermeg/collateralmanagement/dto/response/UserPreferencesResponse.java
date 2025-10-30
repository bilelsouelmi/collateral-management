package com.vermeg.collateralmanagement.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferencesResponse {

    private Long id;
    private Long userId;

    // Notification Preferences
    private Boolean emailNotifications;
    private Boolean alertNotifications;
    private Boolean marginCallNotifications;
    private Boolean reportNotifications;

    // Display Preferences
    private String theme;
    private String language;
    private String dateFormat;
    private String currency;

    // Dashboard Preferences
    private String defaultView;
    private Boolean showWelcomeMessage;

    // Alert Thresholds
    private Double riskAlertThreshold;
    private Double marginCallThreshold;

    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
}