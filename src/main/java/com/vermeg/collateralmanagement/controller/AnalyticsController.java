// AnalyticsControllerEnhanced.java
package com.vermeg.collateralmanagement.controller;

import com.vermeg.collateralmanagement.dto.dashboard.*;
import com.vermeg.collateralmanagement.dto.response.ApiResponse;
import com.vermeg.collateralmanagement.entity.User;
import com.vermeg.collateralmanagement.repository.UserRepository;
import com.vermeg.collateralmanagement.service.AnalyticsService;
import com.vermeg.collateralmanagement.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    // ==================== PORTFOLIO ANALYTICS ====================

    /**
     * Get comprehensive portfolio analytics
     */
    @GetMapping("/portfolio")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PortfolioSummaryDto>> getPortfolioAnalytics(
            Authentication auth,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int periodDays) {

        try {
            log.info("Getting portfolio analytics for user: {} over {} days", auth.getName(), periodDays);
            Long userId = getCurrentUserId(auth);
            PortfolioSummaryDto analytics = dashboardService.getPortfolioSummary(userId);

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Portfolio analytics retrieved for %d day period", periodDays),
                    analytics
            ));
        } catch (Exception e) {
            log.error("Failed to get portfolio analytics: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve portfolio analytics: " + e.getMessage()));
        }
    }

    /**
     * Get portfolio performance metrics
     */
    @GetMapping("/portfolio/performance")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PerformanceMetricsDto>> getPortfolioPerformance(
            Authentication auth,
            @RequestParam(defaultValue = "30") int periodDays,
            @RequestParam(required = false) String benchmark) {

        try {
            log.info("Getting portfolio performance for user: {} over {} days", auth.getName(), periodDays);
            Long userId = getCurrentUserId(auth);

            AnalyticsOverviewDto analytics = dashboardService.getAnalyticsOverview(userId, periodDays);
            PerformanceMetricsDto performance = analytics.getPerformanceMetrics();

            return ResponseEntity.ok(ApiResponse.success(
                    "Portfolio performance metrics retrieved successfully",
                    performance
            ));
        } catch (Exception e) {
            log.error("Failed to get portfolio performance: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve portfolio performance: " + e.getMessage()));
        }
    }

    /**
     * Get portfolio asset allocation breakdown
     */
    @GetMapping("/portfolio/allocation")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<AssetAllocationDto>>> getAssetAllocation(
            Authentication auth,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "false") boolean includeHistorical) {

        try {
            log.info("Getting asset allocation for user: {}", auth.getName());
            Long userId = getCurrentUserId(auth);

            PortfolioSummaryDto portfolioSummary = dashboardService.getPortfolioSummary(userId);
            List<AssetAllocationDto> allocation = portfolioSummary.getAssetAllocation();

            return ResponseEntity.ok(ApiResponse.success(
                    "Asset allocation retrieved successfully",
                    allocation
            ));
        } catch (Exception e) {
            log.error("Failed to get asset allocation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve asset allocation: " + e.getMessage()));
        }
    }

    /**
     * Get portfolio concentration analysis
     */
    @GetMapping("/portfolio/concentration")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConcentrationAnalysis(
            Authentication auth,
            @RequestParam(defaultValue = "0.30") double concentrationThreshold) {

        try {
            log.info("Getting concentration analysis for user: {} with threshold: {}",
                    auth.getName(), concentrationThreshold);
            Long userId = getCurrentUserId(auth);

            Map<String, Object> concentrationData = analyticsService.getConcentrationAnalysis(
                    userId, concentrationThreshold);

            return ResponseEntity.ok(ApiResponse.success(
                    "Concentration analysis completed successfully",
                    concentrationData
            ));
        } catch (Exception e) {
            log.error("Failed to get concentration analysis: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve concentration analysis: " + e.getMessage()));
        }
    }

    // ==================== RISK ANALYTICS ====================

    /**
     * Get comprehensive risk analytics
     */
    @GetMapping("/risk")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<RiskSummaryDto>> getRiskAnalytics(
            Authentication auth,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int periodDays,
            @RequestParam(defaultValue = "false") boolean includeStressTest) {

        try {
            log.info("Getting risk analytics for user: {} over {} days", auth.getName(), periodDays);
            Long userId = getCurrentUserId(auth);

            RiskSummaryDto riskAnalytics = dashboardService.getRiskSummary(userId);

            if (includeStressTest) {
                // Add stress test results
                Map<String, Object> stressTestResults = analyticsService.performStressTest(userId);
                // Note: You would add stress test data to the DTO or create an enhanced version
            }

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Risk analytics retrieved for %d day period", periodDays),
                    riskAnalytics
            ));
        } catch (Exception e) {
            log.error("Failed to get risk analytics: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve risk analytics: " + e.getMessage()));
        }
    }

    /**
     * Get Value at Risk (VaR) analysis
     */
    @GetMapping("/risk/var")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getValueAtRiskAnalysis(
            Authentication auth,
            @RequestParam(defaultValue = "0.95") double confidenceLevel,
            @RequestParam(defaultValue = "1") int timeHorizonDays,
            @RequestParam(defaultValue = "HISTORICAL") String varMethod) {

        try {
            log.info("Getting VaR analysis for user: {} with confidence: {} and method: {}",
                    auth.getName(), confidenceLevel, varMethod);
            Long userId = getCurrentUserId(auth);

            Map<String, Object> varAnalysis = analyticsService.calculateValueAtRisk(
                    userId, confidenceLevel, timeHorizonDays, varMethod);

            return ResponseEntity.ok(ApiResponse.success(
                    "Value at Risk analysis completed successfully",
                    varAnalysis
            ));
        } catch (Exception e) {
            log.error("Failed to get VaR analysis: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve VaR analysis: " + e.getMessage()));
        }
    }

    /**
     * Get stress test scenarios
     */
    @GetMapping("/risk/stress-test")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStressTestResults(
            Authentication auth,
            @RequestParam(defaultValue = "STANDARD") String scenarioType,
            @RequestParam(required = false) List<String> customScenarios) {

        try {
            log.info("Getting stress test results for user: {} with scenario: {}",
                    auth.getName(), scenarioType);
            Long userId = getCurrentUserId(auth);

            Map<String, Object> stressTestResults = analyticsService.performStressTest(
                    userId, scenarioType, customScenarios);

            return ResponseEntity.ok(ApiResponse.success(
                    "Stress test analysis completed successfully",
                    stressTestResults
            ));
        } catch (Exception e) {
            log.error("Failed to get stress test results: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve stress test results: " + e.getMessage()));
        }
    }

    // ==================== ALERT ANALYTICS ====================

    /**
     * Get comprehensive alert analytics
     */
    @GetMapping("/alerts")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<AlertSummaryDto>> getAlertAnalytics(
            Authentication auth,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int periodDays,
            @RequestParam(defaultValue = "false") boolean includePredictions) {

        try {
            log.info("Getting alert analytics for user: {} over {} days", auth.getName(), periodDays);
            Long userId = getCurrentUserId(auth);

            AlertSummaryDto alertAnalytics = dashboardService.getAlertSummary(userId);

            if (includePredictions) {
                // Add predictive analytics for alerts
                Map<String, Object> predictions = analyticsService.getAlertPredictions(userId, periodDays);
                // Note: You would enhance the DTO to include predictions
            }

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Alert analytics retrieved for %d day period", periodDays),
                    alertAnalytics
            ));
        } catch (Exception e) {
            log.error("Failed to get alert analytics: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve alert analytics: " + e.getMessage()));
        }
    }

    /**
     * Get alert pattern analysis
     */
    @GetMapping("/alerts/patterns")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAlertPatterns(
            Authentication auth,
            @RequestParam(defaultValue = "90") int analysisWindowDays,
            @RequestParam(defaultValue = "HOURLY") String granularity) {

        try {
            log.info("Getting alert patterns for user: {} over {} days with {} granularity",
                    auth.getName(), analysisWindowDays, granularity);
            Long userId = getCurrentUserId(auth);

            Map<String, Object> patterns = analyticsService.analyzeAlertPatterns(
                    userId, analysisWindowDays, granularity);

            return ResponseEntity.ok(ApiResponse.success(
                    "Alert pattern analysis completed successfully",
                    patterns
            ));
        } catch (Exception e) {
            log.error("Failed to get alert patterns: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve alert patterns: " + e.getMessage()));
        }
    }

    // ==================== TREND ANALYSIS ====================

    /**
     * Get portfolio value trends
     */
    @GetMapping("/trends/portfolio")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<TrendDataDto>>> getPortfolioTrends(
            Authentication auth,
            @RequestParam(defaultValue = "30") int periodDays,
            @RequestParam(defaultValue = "DAILY") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        try {
            log.info("Getting portfolio trends for user: {} over {} days with {} granularity",
                    auth.getName(), periodDays, granularity);
            Long userId = getCurrentUserId(auth);

            LocalDateTime actualStartDate = startDate != null ? startDate : LocalDateTime.now().minusDays(periodDays);
            LocalDateTime actualEndDate = endDate != null ? endDate : LocalDateTime.now();

            List<TrendDataDto> trends = analyticsService.getPortfolioTrends(
                    userId, actualStartDate, actualEndDate, granularity);

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Portfolio trends retrieved for %d day period", periodDays),
                    trends
            ));
        } catch (Exception e) {
            log.error("Failed to get portfolio trends: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve portfolio trends: " + e.getMessage()));
        }
    }

    /**
     * Get risk score trends
     */
    @GetMapping("/trends/risk")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<TrendDataDto>>> getRiskTrends(
            Authentication auth,
            @RequestParam(defaultValue = "30") int periodDays,
            @RequestParam(defaultValue = "DAILY") String granularity) {

        try {
            log.info("Getting risk trends for user: {} over {} days with {} granularity",
                    auth.getName(), periodDays, granularity);
            Long userId = getCurrentUserId(auth);
            LocalDateTime startDate = LocalDateTime.now().minusDays(periodDays);

            List<TrendDataDto> trends = analyticsService.generateRiskTrends(userId, startDate);

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Risk trends retrieved for %d day period", periodDays),
                    trends
            ));
        } catch (Exception e) {
            log.error("Failed to get risk trends: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve risk trends: " + e.getMessage()));
        }
    }

    /**
     * Get alert frequency trends
     */
    @GetMapping("/trends/alerts")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<TrendDataDto>>> getAlertTrends(
            Authentication auth,
            @RequestParam(defaultValue = "30") int periodDays,
            @RequestParam(defaultValue = "DAILY") String granularity) {

        try {
            log.info("Getting alert trends for user: {} over {} days with {} granularity",
                    auth.getName(), periodDays, granularity);
            Long userId = getCurrentUserId(auth);
            LocalDateTime startDate = LocalDateTime.now().minusDays(periodDays);

            List<TrendDataDto> trends = analyticsService.generateAlertTrendsData(userId, startDate);

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Alert trends retrieved for %d day period", periodDays),
                    trends
            ));
        } catch (Exception e) {
            log.error("Failed to get alert trends: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve alert trends: " + e.getMessage()));
        }
    }

    // ==================== COMPARATIVE ANALYTICS ====================

    /**
     * Get peer comparison analytics
     */
    @GetMapping("/comparison/peer")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPeerComparison(
            Authentication auth,
            @RequestParam(defaultValue = "SIMILAR_SIZE") String peerGroup,
            @RequestParam(defaultValue = "30") int periodDays) {

        try {
            log.info("Getting peer comparison for user: {} with peer group: {}",
                    auth.getName(), peerGroup);
            Long userId = getCurrentUserId(auth);

            Map<String, Object> comparison = analyticsService.performPeerComparison(
                    userId, peerGroup, periodDays);

            return ResponseEntity.ok(ApiResponse.success(
                    "Peer comparison analysis completed successfully",
                    comparison
            ));
        } catch (Exception e) {
            log.error("Failed to get peer comparison: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve peer comparison: " + e.getMessage()));
        }
    }

    /**
     * Get benchmark comparison
     */
    @GetMapping("/comparison/benchmark")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBenchmarkComparison(
            Authentication auth,
            @RequestParam String benchmarkId,
            @RequestParam(defaultValue = "90") int periodDays) {

        try {
            log.info("Getting benchmark comparison for user: {} against benchmark: {}",
                    auth.getName(), benchmarkId);
            Long userId = getCurrentUserId(auth);

            Map<String, Object> comparison = analyticsService.compareToBenchmark(
                    userId, benchmarkId, periodDays);

            return ResponseEntity.ok(ApiResponse.success(
                    "Benchmark comparison completed successfully",
                    comparison
            ));
        } catch (Exception e) {
            log.error("Failed to get benchmark comparison: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve benchmark comparison: " + e.getMessage()));
        }
    }

    // ==================== ADVANCED ANALYTICS ====================

    /**
     * Get correlation analysis
     */
    @GetMapping("/advanced/correlation")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCorrelationAnalysis(
            Authentication auth,
            @RequestParam(defaultValue = "90") int analysisWindow,
            @RequestParam(defaultValue = "ASSET_TYPE") String correlationType) {

        try {
            log.info("Getting correlation analysis for user: {} with window: {} days",
                    auth.getName(), analysisWindow);
            Long userId = getCurrentUserId(auth);

            Map<String, Object> correlationData = analyticsService.performCorrelationAnalysis(
                    userId, analysisWindow, correlationType);

            return ResponseEntity.ok(ApiResponse.success(
                    "Correlation analysis completed successfully",
                    correlationData
            ));
        } catch (Exception e) {
            log.error("Failed to get correlation analysis: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve correlation analysis: " + e.getMessage()));
        }
    }

    /**
     * Get predictive analytics
     */
    @GetMapping("/advanced/predictions")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPredictiveAnalytics(
            Authentication auth,
            @RequestParam(defaultValue = "30") int forecastDays,
            @RequestParam(defaultValue = "PORTFOLIO_VALUE,RISK_SCORE") List<String> metrics) {

        try {
            log.info("Getting predictive analytics for user: {} forecasting {} days",
                    auth.getName(), forecastDays);
            Long userId = getCurrentUserId(auth);

            Map<String, Object> predictions = analyticsService.generatePredictions(
                    userId, forecastDays, metrics);

            return ResponseEntity.ok(ApiResponse.success(
                    "Predictive analytics completed successfully",
                    predictions
            ));
        } catch (Exception e) {
            log.error("Failed to get predictive analytics: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve predictive analytics: " + e.getMessage()));
        }
    }

    // ==================== EXPORT FUNCTIONALITY ====================

    /**
     * Export analytics data
     */
    @PostMapping("/export")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportAnalyticsData(
            Authentication auth,
            @RequestParam(defaultValue = "CSV") String format,
            @RequestParam List<String> dataTypes,
            @RequestParam(defaultValue = "30") int periodDays) {

        try {
            log.info("Exporting analytics data for user: {} in format: {}",
                    auth.getName(), format);
            Long userId = getCurrentUserId(auth);

            Map<String, Object> exportResult = analyticsService.exportAnalyticsData(
                    userId, format, dataTypes, periodDays);

            return ResponseEntity.ok(ApiResponse.success(
                    "Analytics data export completed successfully",
                    exportResult
            ));
        } catch (Exception e) {
            log.error("Failed to export analytics data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to export analytics data: " + e.getMessage()));
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Get current user ID from authentication
     */
    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return user.getId();
    }

    /**
     * Validate date range parameters
     */
    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
    }

    /**
     * Create metadata for analytics responses
     */
    private Map<String, Object> createAnalyticsMetadata(Long userId, int periodDays, String analysisType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("periodDays", periodDays);
        metadata.put("analysisType", analysisType);
        metadata.put("generatedAt", LocalDateTime.now());
        metadata.put("version", "1.0");
        return metadata;
    }
}