package com.vermeg.collateralmanagement.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesRequest {

    // Notification Preferences
    private Boolean emailNotifications;
    private Boolean alertNotifications;
    private Boolean marginCallNotifications;
    private Boolean reportNotifications;

    // Display Preferences
    private String theme; // "light" or "dark"
    private String language; // "en", "fr", etc.
    private String dateFormat; // "DD/MM/YYYY", "MM/DD/YYYY", etc.
    private String currency; // "USD", "EUR", etc.

    // Dashboard Preferences
    private String defaultView; // "dashboard", "portfolios", etc.
    private Boolean showWelcomeMessage;

    // Alert Thresholds
    private Double riskAlertThreshold; // percentage
    private Double marginCallThreshold; // percentage
}