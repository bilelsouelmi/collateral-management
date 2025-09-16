package com.vermeg.collateralmanagement.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesDto {
    private String currency;
    private String timezone;
    private String dateFormat;
    private Boolean enableRealTimeUpdates;
    private Boolean enableEmailNotifications;
    private Boolean enableDesktopNotifications;
    private String dashboardLayout; // DEFAULT, COMPACT, DETAILED
}