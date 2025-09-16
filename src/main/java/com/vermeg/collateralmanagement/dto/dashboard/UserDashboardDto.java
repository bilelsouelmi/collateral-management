package com.vermeg.collateralmanagement.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardDto {
    private Long userId;
    private String username;
    private String fullName;
    private String role;
    private DashboardOverviewDto overview;
    private AnalyticsOverviewDto analytics;
    private SystemHealthDto systemHealth; // Only for admins
    private UserPreferencesDto preferences;
    private LocalDateTime lastLogin;
    private LocalDateTime dataRefreshedAt;
}