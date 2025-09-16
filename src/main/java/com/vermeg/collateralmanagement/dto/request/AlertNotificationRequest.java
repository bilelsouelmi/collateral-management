package com.vermeg.collateralmanagement.dto.request;

import com.vermeg.collateralmanagement.enums.AlertType;
import com.vermeg.collateralmanagement.enums.AlertSeverity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * Request DTO for alert notification settings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertNotificationRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @Builder.Default
    private boolean emailNotifications = true;

    @Builder.Default
    private boolean smsNotifications = false;

    @Builder.Default
    private boolean pushNotifications = true;

    private List<AlertType> notificationTypes; // Types to notify for
    private List<AlertSeverity> notificationSeverities; // Severities to notify for

    @Builder.Default
    private boolean quietHours = false;

    private Integer quietHoursStart; // 22 for 10 PM
    private Integer quietHoursEnd;   // 8 for 8 AM

    @Builder.Default
    private boolean weekendNotifications = true;

    @Builder.Default
    private int maxNotificationsPerHour = 10;

    @Builder.Default
    private boolean consolidateNotifications = true;

    private String emailAddress;
    private String phoneNumber;

    @Builder.Default
    private String notificationLanguage = "en";
}