// ===== DashboardService.java - COMPLETE FIXED VERSION =====
// File: src/main/java/com/vermeg/collateralmanagement/service/DashboardService.java

package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.dto.dashboard.*;
import com.vermeg.collateralmanagement.dto.response.AlertResponse;
import com.vermeg.collateralmanagement.entity.*;
import com.vermeg.collateralmanagement.enums.*;
import com.vermeg.collateralmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final CollateralAssetRepository collateralAssetRepository;
    private final AlertRepository alertRepository;
    private final MarginCallRepository marginCallRepository;
    private final RiskMetricRepository riskMetricRepository;
    private final ReportRepository reportRepository;
    private final ValuationRepository valuationRepository;
    private final AlertService alertService; // ADDED: AlertService dependency

    // ==================== MAIN DASHBOARD METHODS ====================

    /**
     * Get complete dashboard overview for user
     */
    public DashboardOverviewDto getDashboardOverview(String username) {
        log.info("Generating dashboard overview for user: {}", username);

        User user = findUserByUsername(username);

        return DashboardOverviewDto.builder()
                .portfolioSummary(getPortfolioSummary(user.getId()))
                .riskSummary(getRiskSummary(user.getId()))
                .alertSummary(getAlertSummary(user.getId()))
                .marginCallSummary(getMarginCallSummary(user.getId()))
                .recentActivities(getRecentActivities(user.getId()))
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * Get user dashboard with preferences
     */
    public UserDashboardDto getUserDashboard(String username) {
        log.info("Generating user dashboard for: {}", username);

        User user = findUserByUsername(username);

        return UserDashboardDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole().getName())
                .overview(getDashboardOverview(username))
                .analytics(getAnalyticsOverview(user.getId(), 30))
                .systemHealth(isAdminUser(user) ? getSystemHealth() : null)
                .preferences(getUserPreferences(user.getId()))
                .lastLogin(user.getLastLogin())
                .dataRefreshedAt(LocalDateTime.now())
                .build();
    }

    // ==================== PORTFOLIO SUMMARY ====================

    /**
     * Get portfolio summary for user
     */
    public PortfolioSummaryDto getPortfolioSummary(Long userId) {
        log.debug("Generating portfolio summary for user: {}", userId);

        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        List<CollateralAsset> allAssets = collateralAssetRepository.findByUserId(userId);

        BigDecimal totalValue = portfolios.stream()
                .map(Portfolio::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMargin = portfolios.stream()
                .map(Portfolio::getTotalMargin)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate daily change (simplified - in real app would use historical data)
        BigDecimal dailyChange = calculateDailyChange(allAssets);
        Double dailyChangePercentage = calculateDailyChangePercentage(totalValue, dailyChange);

        return PortfolioSummaryDto.builder()
                .totalValue(totalValue)
                .totalMargin(totalMargin)
                .totalPortfolios(portfolios.size())
                .totalAssets(allAssets.size())
                .portfolioBreakdown(createPortfolioBreakdown(portfolios))
                .assetAllocation(calculateAssetAllocation(allAssets))
                .dailyChange(dailyChange)
                .dailyChangePercentage(dailyChangePercentage)
                .build();
    }

    /**
     * Create portfolio breakdown
     */
    private List<PortfolioBreakdownDto> createPortfolioBreakdown(List<Portfolio> portfolios) {
        return portfolios.stream()
                .map(portfolio -> {
                    List<CollateralAsset> assets = collateralAssetRepository.findByPortfolioId(portfolio.getId());

                    return PortfolioBreakdownDto.builder()
                            .portfolioId(portfolio.getId())
                            .portfolioName(portfolio.getName())
                            .portfolioType(portfolio.getType().getDisplayName())
                            .totalValue(portfolio.getTotalValue())
                            .totalMargin(portfolio.getTotalMargin())
                            .assetCount(assets.size())
                            .status("ACTIVE") // Could be calculated based on portfolio state
                            .build();
                })
                .sorted((p1, p2) -> p2.getTotalValue().compareTo(p1.getTotalValue()))
                .collect(Collectors.toList());
    }

    /**
     * Calculate asset allocation
     */
    private List<AssetAllocationDto> calculateAssetAllocation(List<CollateralAsset> assets) {
        if (assets.isEmpty()) {
            return new ArrayList<>();
        }

        Map<AssetType, List<CollateralAsset>> assetsByType = assets.stream()
                .collect(Collectors.groupingBy(CollateralAsset::getType));

        BigDecimal totalValue = assets.stream()
                .map(CollateralAsset::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return assetsByType.entrySet().stream()
                .map(entry -> {
                    AssetType type = entry.getKey();
                    List<CollateralAsset> typeAssets = entry.getValue();

                    BigDecimal typeValue = typeAssets.stream()
                            .map(CollateralAsset::getMarketValue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    Double percentage = totalValue.compareTo(BigDecimal.ZERO) == 0 ? 0.0 :
                            typeValue.divide(totalValue, 4, RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal("100"))
                                    .doubleValue();

                    return AssetAllocationDto.builder()
                            .assetType(type.name())
                            .displayName(type.getDisplayName())
                            .value(typeValue)
                            .percentage(percentage)
                            .count(typeAssets.size())
                            .status("ACTIVE")
                            .build();
                })
                .sorted((a1, a2) -> a2.getValue().compareTo(a1.getValue()))
                .collect(Collectors.toList());
    }

    // ==================== RISK SUMMARY ====================

    /**
     * Get risk summary for user
     */
    public RiskSummaryDto getRiskSummary(Long userId) {
        log.debug("Generating risk summary for user: {}", userId);

        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        List<RiskMetric> latestRiskMetrics = riskMetricRepository.findLatestByUserId(userId);

        BigDecimal overallRiskScore = calculateOverallRiskScore(latestRiskMetrics);
        String riskRating = determineRiskRating(overallRiskScore);

        BigDecimal valueAtRisk = calculateValueAtRisk(portfolios);
        Double utilizationPercentage = calculateUtilizationPercentage(portfolios);

        Integer portfoliosAtRisk = (int) latestRiskMetrics.stream()
                .mapToLong(rm -> rm.getValue().compareTo(new BigDecimal("0.8")) >= 0 ? 1 : 0)
                .sum();

        return RiskSummaryDto.builder()
                .overallRiskScore(overallRiskScore)
                .riskRating(riskRating)
                .valueAtRisk(valueAtRisk)
                .utilizationPercentage(utilizationPercentage)
                .portfoliosAtRisk(portfoliosAtRisk)
                .riskMetrics(convertToRiskMetricDtos(latestRiskMetrics))
                .riskAlerts(getRiskAlerts(userId))
                .lastCalculated(getLatestRiskCalculationDate(latestRiskMetrics))
                .build();
    }

    /**
     * Convert risk metrics to DTOs
     */
    private List<RiskMetricDto> convertToRiskMetricDtos(List<RiskMetric> riskMetrics) {
        return riskMetrics.stream()
                .map(metric -> RiskMetricDto.builder()
                        .portfolioId(metric.getPortfolio().getId())
                        .portfolioName(metric.getPortfolio().getName())
                        .currentRisk(metric.getValue())
                        .threshold(new BigDecimal("0.8")) // Standard threshold
                        .status(determineRiskStatus(metric.getValue()))
                        .methodology(metric.getMethodology())
                        .lastCalculated(metric.getCalculationDate())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get risk-related alerts - FIXED to avoid lazy loading
     */
    private List<RiskAlertDto> getRiskAlerts(Long userId) {
        // FIXED: Use AlertService to get DTOs instead of entities
        List<AlertResponse> riskAlertDtos = alertService.getAlertsByTypeAsDto(userId, AlertType.THRESHOLD_BREACH)
                .stream()
                .limit(5)
                .collect(Collectors.toList());

        return riskAlertDtos.stream()
                .map(alertDto -> RiskAlertDto.builder()
                        .alertId(alertDto.getId())
                        .alertType(alertDto.getType().name())
                        .severity(alertDto.getSeverity().name())
                        .message(alertDto.getMessage())
                        .portfolioId(null) // Would need portfolio context from alert
                        .portfolioName("Portfolio") // Would extract from alert
                        .triggeredAt(alertDto.getTriggeredAt())
                        .isRead(alertDto.isRead())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== ALERT SUMMARY - FIXED ====================

    /**
     * Get alert summary for user - FIXED to avoid lazy loading
     */
    public AlertSummaryDto getAlertSummary(Long userId) {
        log.debug("Generating alert summary for user: {}", userId);

        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Object[]> alertStatsResult = alertRepository.getAlertSummaryByUser(userId, since);

        // Handle empty result or get first row
        Object[] alertStats;
        if (alertStatsResult.isEmpty()) {
            // Return default values if no data
            return AlertSummaryDto.builder()
                    .totalAlerts(0)
                    .criticalAlerts(0)
                    .highAlerts(0)
                    .mediumAlerts(0)
                    .lowAlerts(0)
                    .unreadAlerts(0)
                    .recentAlerts(new ArrayList<>())
                    .alertTrends(getAlertTrends(userId))
                    .build();
        } else {
            alertStats = alertStatsResult.get(0);
        }

        // Parse native query results with proper type conversion
        Long total = alertStats[0] != null ? ((Number) alertStats[0]).longValue() : 0L;
        Long critical = alertStats[1] != null ? ((Number) alertStats[1]).longValue() : 0L;
        Long high = alertStats[2] != null ? ((Number) alertStats[2]).longValue() : 0L;
        Long medium = alertStats[3] != null ? ((Number) alertStats[3]).longValue() : 0L;
        Long low = alertStats[4] != null ? ((Number) alertStats[4]).longValue() : 0L;
        Long unread = alertStats[5] != null ? ((Number) alertStats[5]).longValue() : 0L;

        // FIXED: Use AlertService to get DTOs instead of entities
        List<AlertResponse> recentAlertDtos = alertService.getUserAlertsAsDto(userId, false)
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        return AlertSummaryDto.builder()
                .totalAlerts(total.intValue())
                .criticalAlerts(critical.intValue())
                .highAlerts(high.intValue())
                .mediumAlerts(medium.intValue())
                .lowAlerts(low.intValue())
                .unreadAlerts(unread.intValue())
                .recentAlerts(convertAlertResponseToOverviewDtos(recentAlertDtos)) // FIXED method call
                .alertTrends(getAlertTrends(userId))
                .build();
    }

    /**
     * Convert AlertResponse DTOs to AlertOverviewDtos - FIXED
     */
    private List<AlertOverviewDto> convertAlertResponseToOverviewDtos(List<AlertResponse> alertResponses) {
        return alertResponses.stream()
                .map(alertResponse -> AlertOverviewDto.builder()
                        .id(alertResponse.getId())
                        .type(alertResponse.getType().name())
                        .severity(alertResponse.getSeverity().name())
                        .title(alertResponse.getTitle())
                        .message(alertResponse.getMessage())
                        .isRead(alertResponse.isRead())
                        .createdAt(alertResponse.getCreatedAt())
                        .triggeredAt(alertResponse.getTriggeredAt())
                        .minutesSinceCreated(alertResponse.getMinutesSinceTriggered())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get alert trends
     */
    private AlertTrendsDto getAlertTrends(Long userId) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);

        try {
            List<Object[]> trendData = alertRepository.getAlertTrendsByUser(userId, startDate);

            List<AlertTrendDataDto> dailyTrends = trendData.stream()
                    .map(data -> {
                        LocalDateTime date;
                        if (data[0] instanceof java.sql.Date) {
                            date = ((java.sql.Date) data[0]).toLocalDate().atStartOfDay();
                        } else if (data[0] instanceof java.sql.Timestamp) {
                            date = ((java.sql.Timestamp) data[0]).toLocalDateTime();
                        } else {
                            date = LocalDateTime.now();
                        }

                        return AlertTrendDataDto.builder()
                                .date(date)
                                .totalAlerts(((Number) data[1]).intValue())
                                .criticalAlerts(((Number) data[2]).intValue())
                                .unreadAlerts(((Number) data[3]).intValue())
                                .build();
                    })
                    .collect(Collectors.toList());

            double avgAlertsPerDay = dailyTrends.stream()
                    .mapToInt(AlertTrendDataDto::getTotalAlerts)
                    .average()
                    .orElse(0.0);

            return AlertTrendsDto.builder()
                    .dailyTrends(dailyTrends)
                    .averageAlertsPerDay(avgAlertsPerDay)
                    .resolutionRate(85.0)
                    .averageResolutionTimeMinutes(45L)
                    .build();

        } catch (Exception e) {
            log.error("Error getting alert trends for user {}: {}", userId, e.getMessage());
            // Return empty trends on error
            return AlertTrendsDto.builder()
                    .dailyTrends(new ArrayList<>())
                    .averageAlertsPerDay(0.0)
                    .resolutionRate(0.0)
                    .averageResolutionTimeMinutes(0L)
                    .build();
        }
    }
    // ==================== MARGIN CALL SUMMARY ====================

    /**
     * Get margin call summary for user
     */
    public MarginCallSummaryDto getMarginCallSummary(Long userId) {
        log.debug("Generating margin call summary for user: {}", userId);

        LocalDateTime since = LocalDateTime.now().minusDays(30);

        try {
            Object[] mcStats = marginCallRepository.getMarginCallSummaryByUser(userId, since);

            // Handle null result
            if (mcStats == null || mcStats.length == 0) {
                return MarginCallSummaryDto.builder()
                        .totalMarginCalls(0)
                        .activeMarginCalls(0)
                        .overdueMarginCalls(0)
                        .acknowledgedMarginCalls(0)
                        .totalShortfall(BigDecimal.ZERO)
                        .averageShortfall(BigDecimal.ZERO)
                        .recentMarginCalls(new ArrayList<>())
                        .urgentMarginCalls(new ArrayList<>())
                        .build();
            }

            // Debug logging to see what we're getting
            log.debug("mcStats array length: {}", mcStats.length);
            for (int i = 0; i < mcStats.length; i++) {
                log.debug("mcStats[{}] = {} (type: {})", i, mcStats[i],
                        mcStats[i] != null ? mcStats[i].getClass().getSimpleName() : "null");
            }

            // Parse native query results with proper type conversion and additional safety
            Long total = 0L;
            Long active = 0L;
            Long overdue = 0L;
            BigDecimal totalShortfall = BigDecimal.ZERO;
            BigDecimal avgShortfall = BigDecimal.ZERO;

            if (mcStats[0] != null) {
                total = ((Number) mcStats[0]).longValue();
            }
            if (mcStats[1] != null) {
                active = ((Number) mcStats[1]).longValue();
            }
            if (mcStats[2] != null) {
                overdue = ((Number) mcStats[2]).longValue();
            }
            if (mcStats[3] != null) {
                if (mcStats[3] instanceof BigDecimal) {
                    totalShortfall = (BigDecimal) mcStats[3];
                } else if (mcStats[3] instanceof Number) {
                    totalShortfall = BigDecimal.valueOf(((Number) mcStats[3]).doubleValue());
                }
            }
            if (mcStats[4] != null) {
                if (mcStats[4] instanceof BigDecimal) {
                    avgShortfall = (BigDecimal) mcStats[4];
                } else if (mcStats[4] instanceof Number) {
                    avgShortfall = BigDecimal.valueOf(((Number) mcStats[4]).doubleValue());
                }
            }

            List<MarginCall> recentMarginCalls = marginCallRepository.findActiveMarginCallsByUserId(userId)
                    .stream()
                    .limit(5)
                    .collect(Collectors.toList());

            List<MarginCall> urgentCalls = marginCallRepository.findUrgentMarginCallsByUser(userId, 24);

            return MarginCallSummaryDto.builder()
                    .totalMarginCalls(total.intValue())
                    .activeMarginCalls(active.intValue())
                    .overdueMarginCalls(overdue.intValue())
                    .acknowledgedMarginCalls(0)
                    .totalShortfall(totalShortfall)
                    .averageShortfall(avgShortfall)
                    .recentMarginCalls(convertToMarginCallOverviewDtos(recentMarginCalls))
                    .urgentMarginCalls(convertToUrgentMarginCallDtos(urgentCalls))
                    .build();

        } catch (Exception e) {
            log.error("Error in getMarginCallSummary for user {}: {}", userId, e.getMessage(), e);
            // Return default values on error
            return MarginCallSummaryDto.builder()
                    .totalMarginCalls(0)
                    .activeMarginCalls(0)
                    .overdueMarginCalls(0)
                    .acknowledgedMarginCalls(0)
                    .totalShortfall(BigDecimal.ZERO)
                    .averageShortfall(BigDecimal.ZERO)
                    .recentMarginCalls(new ArrayList<>())
                    .urgentMarginCalls(new ArrayList<>())
                    .build();
        }
    }

    /**
     * Convert margin calls to overview DTOs
     */
    private List<MarginCallOverviewDto> convertToMarginCallOverviewDtos(List<MarginCall> marginCalls) {
        return marginCalls.stream()
                .map(mc -> MarginCallOverviewDto.builder()
                        .id(mc.getId())
                        .portfolioId(mc.getPortfolio().getId())
                        .portfolioName(mc.getPortfolio().getName())
                        .requiredMargin(mc.getRequiredMargin())
                        .currentMargin(mc.getCurrentMargin())
                        .shortfall(mc.getShortfall())
                        .status(mc.getStatus().name())
                        .dueDate(mc.getDueDate())
                        .isOverdue(mc.isOverdue())
                        .daysUntilDue(mc.getDaysUntilDue())
                        .urgencyLevel(determineUrgencyLevel(mc))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Convert to urgent margin call DTOs
     */
    private List<UrgentMarginCallDto> convertToUrgentMarginCallDtos(List<MarginCall> marginCalls) {
        return marginCalls.stream()
                .map(mc -> UrgentMarginCallDto.builder()
                        .id(mc.getId())
                        .portfolioName(mc.getPortfolio().getName())
                        .shortfall(mc.getShortfall())
                        .dueDate(mc.getDueDate())
                        .hoursUntilDue(calculateHoursUntilDue(mc.getDueDate()))
                        .urgencyLevel(determineUrgencyLevel(mc))
                        .recommendedAction(generateRecommendedAction(mc))
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== RECENT ACTIVITIES - FIXED ====================

    /**
     * Get recent activities for user - FIXED to avoid lazy loading for alerts
     */
    public List<RecentActivityDto> getRecentActivities(Long userId) {
        log.debug("Generating recent activities for user: {}", userId);

        List<RecentActivityDto> activities = new ArrayList<>();

        // Recent portfolios (unchanged)
        List<Portfolio> recentPortfolios = portfolioRepository.findByUserId(userId)
                .stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .limit(3)
                .collect(Collectors.toList());

        for (Portfolio portfolio : recentPortfolios) {
            activities.add(RecentActivityDto.builder()
                    .activityType("PORTFOLIO_CREATED")
                    .description("Portfolio '" + portfolio.getName() + "' was created")
                    .entityType("PORTFOLIO")
                    .entityId(portfolio.getId())
                    .entityName(portfolio.getName())
                    .status("ACTIVE")
                    .severity("INFO")
                    .icon("portfolio")
                    .actionUrl("/portfolios/" + portfolio.getId())
                    .timestamp(portfolio.getCreatedAt())
                    .minutesAgo(ChronoUnit.MINUTES.between(portfolio.getCreatedAt(), LocalDateTime.now()))
                    .build());
        }

        // FIXED: Recent alerts using DTO service
        List<AlertResponse> recentAlertDtos = alertService.getUserAlertsAsDto(userId, false)
                .stream()
                .limit(5)
                .collect(Collectors.toList());

        for (AlertResponse alertDto : recentAlertDtos) {
            activities.add(RecentActivityDto.builder()
                    .activityType("ALERT_TRIGGERED")
                    .description("Alert: " + alertDto.getTitle())
                    .entityType("ALERT")
                    .entityId(alertDto.getId())
                    .entityName(alertDto.getTitle())
                    .status(alertDto.isRead() ? "read" : "UNREAD")
                    .severity(alertDto.getSeverity().name())
                    .icon("alert")
                    .actionUrl("/alerts/" + alertDto.getId())
                    .timestamp(alertDto.getCreatedAt())
                    .minutesAgo(alertDto.getMinutesSinceTriggered())
                    .build());
        }

        // Recent reports (unchanged)
        List<Report> recentReports = reportRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(3)
                .collect(Collectors.toList());

        for (Report report : recentReports) {
            activities.add(RecentActivityDto.builder()
                    .activityType("REPORT_GENERATED")
                    .description("Report '" + report.getName() + "' was generated")
                    .entityType("REPORT")
                    .entityId(report.getId())
                    .entityName(report.getName())
                    .status(report.isGenerated() ? "COMPLETED" : "PENDING")
                    .severity("INFO")
                    .icon("report")
                    .actionUrl("/reports/" + report.getId())
                    .timestamp(report.getCreatedAt())
                    .minutesAgo(ChronoUnit.MINUTES.between(report.getCreatedAt(), LocalDateTime.now()))
                    .build());
        }

        return activities.stream()
                .sorted((a1, a2) -> a2.getTimestamp().compareTo(a1.getTimestamp()))
                .limit(15)
                .collect(Collectors.toList());
    }

    // ==================== ANALYTICS OVERVIEW ====================

    /**
     * Get analytics overview
     */
    public AnalyticsOverviewDto getAnalyticsOverview(Long userId, int periodDays) {
        log.debug("Generating analytics overview for user: {} over {} days", userId, periodDays);

        LocalDateTime startDate = LocalDateTime.now().minusDays(periodDays);
        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);

        return AnalyticsOverviewDto.builder()
                .performanceMetrics(calculatePerformanceMetrics(portfolios))
                .portfolioTrends(generatePortfolioTrends(portfolios, startDate))
                .riskTrends(generateRiskTrends(userId, startDate))
                .alertTrends(generateAlertTrendsData(userId, startDate))
                .dataAsOf(LocalDateTime.now())
                .periodDays(periodDays)
                .build();
    }

    // ==================== SYSTEM HEALTH ====================

    /**
     * Get system health (admin only)
     */
    public SystemHealthDto getSystemHealth() {
        log.debug("Generating system health status");

        Map<String, String> componentStatus = new HashMap<>();
        componentStatus.put("database", "HEALTHY");
        componentStatus.put("application", "HEALTHY");
        componentStatus.put("external_apis", "HEALTHY");

        List<String> activeIssues = new ArrayList<>();
        List<String> recommendations = List.of(
                "System is operating normally",
                "Monitor daily report generation",
                "Review alert patterns weekly"
        );

        return SystemHealthDto.builder()
                .overallStatus("HEALTHY")
                .healthScore(95.0)
                .lastChecked(LocalDateTime.now())
                .componentStatus(componentStatus)
                .activeIssues(activeIssues)
                .recommendations(recommendations)
                .performance(SystemPerformanceDto.builder()
                        .responseTime(150.0)
                        .throughput(50.0)
                        .errorRate(0.1)
                        .uptime(99.9)
                        .activeUsers(15)
                        .concurrentSessions(8)
                        .totalRequests(10000L)
                        .successfulRequests(9990L)
                        .build())
                .build();
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private boolean isAdminUser(User user) {
        return user.getRole() != null &&
                user.getRole().getType() == RoleType.ADMINISTRATOR;
    }

    private UserPreferencesDto getUserPreferences(Long userId) {
        // Mock preferences - in real app would come from database
        return UserPreferencesDto.builder()
                .currency("USD")
                .timezone("UTC")
                .dateFormat("yyyy-MM-dd")
                .enableRealTimeUpdates(true)
                .enableEmailNotifications(true)
                .enableDesktopNotifications(false)
                .dashboardLayout("DEFAULT")
                .build();
    }

    // Additional helper methods for calculations...
    private BigDecimal calculateDailyChange(List<CollateralAsset> assets) {
        // Simplified calculation - in real implementation would use historical data
        return assets.stream()
                .map(asset -> asset.getMarketValue().multiply(new BigDecimal("0.015"))) // 1.5% mock change
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Double calculateDailyChangePercentage(BigDecimal totalValue, BigDecimal dailyChange) {
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return dailyChange.divide(totalValue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    private BigDecimal calculateOverallRiskScore(List<RiskMetric> riskMetrics) {
        if (riskMetrics.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalRisk = riskMetrics.stream()
                .map(RiskMetric::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalRisk.divide(new BigDecimal(riskMetrics.size()), 4, RoundingMode.HALF_UP);
    }

    private String determineRiskRating(BigDecimal riskScore) {
        if (riskScore.compareTo(new BigDecimal("0.8")) >= 0) {
            return "CRITICAL";
        } else if (riskScore.compareTo(new BigDecimal("0.6")) >= 0) {
            return "HIGH";
        } else if (riskScore.compareTo(new BigDecimal("0.3")) >= 0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private String determineRiskStatus(BigDecimal riskValue) {
        if (riskValue.compareTo(new BigDecimal("0.8")) >= 0) {
            return "CRITICAL";
        } else if (riskValue.compareTo(new BigDecimal("0.6")) >= 0) {
            return "WARNING";
        } else {
            return "NORMAL";
        }
    }

    private BigDecimal calculateValueAtRisk(List<Portfolio> portfolios) {
        BigDecimal totalValue = portfolios.stream()
                .map(Portfolio::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalValue.multiply(new BigDecimal("0.05")); // 5% VaR
    }

    private Double calculateUtilizationPercentage(List<Portfolio> portfolios) {
        BigDecimal totalValue = portfolios.stream()
                .map(Portfolio::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMargin = portfolios.stream()
                .map(Portfolio::getTotalMargin)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        return totalMargin.divide(totalValue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    private LocalDateTime getLatestRiskCalculationDate(List<RiskMetric> riskMetrics) {
        return riskMetrics.stream()
                .map(RiskMetric::getCalculationDate)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private String determineUrgencyLevel(MarginCall marginCall) {
        if (marginCall.isOverdue()) {
            return "CRITICAL";
        } else if (marginCall.getDaysUntilDue() <= 1) {
            return "HIGH";
        } else if (marginCall.getDaysUntilDue() <= 3) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private Long calculateHoursUntilDue(LocalDateTime dueDate) {
        if (dueDate == null) return 0L;
        return ChronoUnit.HOURS.between(LocalDateTime.now(), dueDate);
    }

    private String generateRecommendedAction(MarginCall marginCall) {
        if (marginCall.isOverdue()) {
            return "Immediate action required - margin call is overdue";
        } else if (marginCall.getDaysUntilDue() <= 1) {
            return "Add collateral or reduce exposure within 24 hours";
        } else {
            return "Review portfolio positions and prepare additional collateral";
        }
    }

    // Mock methods for analytics data
    private PerformanceMetricsDto calculatePerformanceMetrics(List<Portfolio> portfolios) {
        BigDecimal totalValue = portfolios.stream()
                .map(Portfolio::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PerformanceMetricsDto.builder()
                .totalReturn(totalValue.multiply(new BigDecimal("0.08")))
                .totalReturnPercentage(8.5)
                .volatility(12.3)
                .sharpeRatio(1.2)
                .maxDrawdown(5.8)
                .valueAtRisk(totalValue.multiply(new BigDecimal("0.05")))
                .performanceRating("GOOD")
                .build();
    }

    private List<TrendDataDto> generatePortfolioTrends(List<Portfolio> portfolios, LocalDateTime startDate) {
        List<TrendDataDto> trends = new ArrayList<>();
        LocalDateTime date = startDate;

        BigDecimal baseValue = portfolios.stream()
                .map(Portfolio::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        while (date.isBefore(LocalDateTime.now())) {
            // Mock trend data - in real implementation would query historical data
            BigDecimal variance = new BigDecimal(Math.random() * 0.1 - 0.05); // ±5% variance
            BigDecimal value = baseValue.multiply(BigDecimal.ONE.add(variance));

            trends.add(TrendDataDto.builder()
                    .date(date)
                    .value(value)
                    .label("Portfolio Value")
                    .category("PORTFOLIO")
                    .build());

            date = date.plusDays(1);
        }

        return trends;
    }

    private List<TrendDataDto> generateRiskTrends(Long userId, LocalDateTime startDate) {
        List<TrendDataDto> trends = new ArrayList<>();
        LocalDateTime date = startDate;

        while (date.isBefore(LocalDateTime.now())) {
            // Mock risk trend data
            BigDecimal riskValue = new BigDecimal(0.3 + Math.random() * 0.4); // Random between 0.3-0.7

            trends.add(TrendDataDto.builder()
                    .date(date)
                    .value(riskValue)
                    .label("Risk Score")
                    .category("RISK")
                    .build());

            date = date.plusDays(1);
        }

        return trends;
    }

    private List<TrendDataDto> generateAlertTrendsData(Long userId, LocalDateTime startDate) {
        List<TrendDataDto> trends = new ArrayList<>();

        // Get actual alert trends from repository
        List<Object[]> alertTrends = alertRepository.getAlertTrendsByUser(userId, startDate);

        for (Object[] trend : alertTrends) {
            LocalDateTime date = (LocalDateTime) trend[0];
            Integer count = ((Number) trend[1]).intValue();

            trends.add(TrendDataDto.builder()
                    .date(date)
                    .value(new BigDecimal(count))
                    .label("Alert Count")
                    .category("ALERTS")
                    .build());
        }

        return trends;
    }
}