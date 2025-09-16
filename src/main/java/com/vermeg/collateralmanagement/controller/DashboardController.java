package com.vermeg.collateralmanagement.controller;

import com.vermeg.collateralmanagement.dto.dashboard.*;
import com.vermeg.collateralmanagement.entity.User;
import com.vermeg.collateralmanagement.repository.UserRepository;
import com.vermeg.collateralmanagement.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    /**
     * Get complete dashboard overview
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<DashboardOverviewDto> getDashboardOverview(Authentication auth) {
        log.info("Getting dashboard overview for user: {}", auth.getName());
        DashboardOverviewDto overview = dashboardService.getDashboardOverview(auth.getName());
        return ResponseEntity.ok(overview);
    }

    /**
     * Get user dashboard with preferences
     */
    @GetMapping("/user")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<UserDashboardDto> getUserDashboard(Authentication auth) {
        log.info("Getting user dashboard for: {}", auth.getName());
        UserDashboardDto dashboard = dashboardService.getUserDashboard(auth.getName());
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Get portfolio summary
     */
    @GetMapping("/portfolio-summary")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<PortfolioSummaryDto> getPortfolioSummary(Authentication auth) {
        log.info("Getting portfolio summary for user: {}", auth.getName());
        Long userId = getCurrentUserId(auth);
        PortfolioSummaryDto summary = dashboardService.getPortfolioSummary(userId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get risk summary
     */
    @GetMapping("/risk-summary")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<RiskSummaryDto> getRiskSummary(Authentication auth) {
        log.info("Getting risk summary for user: {}", auth.getName());
        Long userId = getCurrentUserId(auth);
        RiskSummaryDto riskSummary = dashboardService.getRiskSummary(userId);
        return ResponseEntity.ok(riskSummary);
    }

    /**
     * Get alert summary
     */
    @GetMapping("/alert-summary")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<AlertSummaryDto> getAlertSummary(Authentication auth) {
        log.info("Getting alert summary for user: {}", auth.getName());
        Long userId = getCurrentUserId(auth);
        AlertSummaryDto alertSummary = dashboardService.getAlertSummary(userId);
        return ResponseEntity.ok(alertSummary);
    }

    /**
     * Get margin call summary
     */
    @GetMapping("/margin-call-summary")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<MarginCallSummaryDto> getMarginCallSummary(Authentication auth) {
        log.info("Getting margin call summary for user: {}", auth.getName());
        Long userId = getCurrentUserId(auth);
        MarginCallSummaryDto marginCallSummary = dashboardService.getMarginCallSummary(userId);
        return ResponseEntity.ok(marginCallSummary);
    }

    /**
     * Get recent activities
     */
    @GetMapping("/recent-activities")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<List<RecentActivityDto>> getRecentActivities(
            Authentication auth,
            @RequestParam(defaultValue = "15") int limit) {
        log.info("Getting recent activities for user: {}", auth.getName());
        Long userId = getCurrentUserId(auth);
        List<RecentActivityDto> activities = dashboardService.getRecentActivities(userId);
        return ResponseEntity.ok(activities.stream().limit(limit).toList());
    }

    /**
     * Get analytics overview
     */
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<AnalyticsOverviewDto> getAnalyticsOverview(
            Authentication auth,
            @RequestParam(defaultValue = "30") int periodDays) {
        log.info("Getting analytics overview for user: {} over {} days", auth.getName(), periodDays);
        Long userId = getCurrentUserId(auth);
        AnalyticsOverviewDto analytics = dashboardService.getAnalyticsOverview(userId, periodDays);
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get system health (admin only)
     */
    @GetMapping("/system-health")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<SystemHealthDto> getSystemHealth() {
        log.info("Getting system health status");
        SystemHealthDto systemHealth = dashboardService.getSystemHealth();
        return ResponseEntity.ok(systemHealth);
    }

    /**
     * Get current user ID from authentication
     */
    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return user.getId();
    }
}