// ===== DashboardService.java =====
// File: src/main/java/com/vermeg/collateralmanagement/service/DashboardService.java

package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.dto.dashboard.*;
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
     * Get risk-related alerts
     */
    private List<RiskAlertDto> getRiskAlerts(Long userId) {
        List<Alert> riskAlerts = alertRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, AlertType.THRESHOLD_BREACH)
                .stream()
                .limit(5)
                .collect(Collectors.toList());

        return riskAlerts.stream()
                .map(alert -> RiskAlertDto.builder()
                        .alertId(alert.getId())
                        .alertType(alert.getType().name())
                        .severity(alert.getSeverity().name())
                        .message(alert.getMessage())
                        .portfolioId(null) // Would need portfolio context from alert
                        .portfolioName("Portfolio") // Would extract from alert
                        .triggeredAt(alert.getTriggeredAt())
                        .isRead(alert.getIsRead())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== ALERT SUMMARY ====================

    /**
     * Get alert summary for user
     */
    public AlertSummaryDto getAlertSummary(Long userId) {
        log.debug("Generating alert summary for user: {}", userId);

        LocalDateTime since = LocalDateTime.now().minusDays(30);
        Object[] alertStats = alertRepository.getAlertSummaryByUser(userId, since);

        // Parse native query results: total, critical, high, medium, low, unread
        Long total = (Long) alertStats[0];
        Long critical = (Long) alertStats[1];
        Long high = (Long) alertStats[2];
        Long medium = (Long) alertStats[3];
        Long low = (Long) alertStats[4];
        Long unread = (Long) alertStats[5];

        List<Alert> recentAlerts = alertRepository.findByUserIdOrderByCreatedAtDesc(userId)
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
                .recentAlerts(convertToAlertOverviewDtos(recentAlerts))
                .alertTrends(getAlertTrends(userId))
                .build();
    }

    /**
     * Convert alerts to overview DTOs
     */
    private List<AlertOverviewDto> convertToAlertOverviewDtos(List<Alert> alerts) {
        return alerts.stream()
                .map(alert -> AlertOverviewDto.builder()
                        .id(alert.getId())
                        .type(alert.getType().name())
                        .severity(alert.getSeverity().name())
                        .title(alert.getTitle())
                        .message(alert.getMessage())
                        .isRead(alert.getIsRead())
                        .createdAt(alert.getCreatedAt())
                        .triggeredAt(alert.getTriggeredAt())
                        .minutesSinceCreated(ChronoUnit.MINUTES.between(alert.getCreatedAt(), LocalDateTime.now()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get alert trends
     */
    private AlertTrendsDto getAlertTrends(Long userId) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        List<Object[]> trendData = alertRepository.getAlertTrendsByUser(userId, startDate);

        List<AlertTrendDataDto> dailyTrends = trendData.stream()
                .map(data -> AlertTrendDataDto.builder()
                        .date((LocalDateTime) data[0])
                        .totalAlerts(((Number) data[1]).intValue())
                        .criticalAlerts(((Number) data[2]).intValue())
                        .unreadAlerts(((Number) data[3]).intValue())
                        .build())
                .collect(Collectors.toList());

        // Calculate metrics
        double avgAlertsPerDay = dailyTrends.stream()
                .mapToInt(AlertTrendDataDto::getTotalAlerts)
                .average()
                .orElse(0.0);

        return AlertTrendsDto.builder()
                .dailyTrends(dailyTrends)
                .averageAlertsPerDay(avgAlertsPerDay)
                .resolutionRate(85.0) // Mock calculation
                .averageResolutionTimeMinutes(45L) // Mock calculation
                .build();
    }

    // ==================== MARGIN CALL SUMMARY ====================

    /**
     * Get margin call summary for user
     */
    public MarginCallSummaryDto getMarginCallSummary(Long userId) {
        log.debug("Generating margin call summary for user: {}", userId);

        LocalDateTime since = LocalDateTime.now().minusDays(30);
        Object[] mcStats = marginCallRepository.getMarginCallSummaryByUser(userId, since);

        // Parse native query results: total, active, overdue, totalShortfall, avgShortfall
        Long total = (Long) mcStats[0];
        Long active = (Long) mcStats[1];
        Long overdue = (Long) mcStats[2];
        BigDecimal totalShortfall = (BigDecimal) mcStats[3];
        BigDecimal avgShortfall = (BigDecimal) mcStats[4];

        List<MarginCall> recentMarginCalls = marginCallRepository.findActiveMarginCallsByUserId(userId)
                .stream()
                .limit(5)
                .collect(Collectors.toList());

        List<MarginCall> urgentCalls = marginCallRepository.findUrgentMarginCallsByUser(userId, 24);

        return MarginCallSummaryDto.builder()
                .totalMarginCalls(total.intValue())
                .activeMarginCalls(active.intValue())
                .overdueMarginCalls(overdue.intValue())
                .acknowledgedMarginCalls(0) // Would calculate from status
                .totalShortfall(totalShortfall)
                .averageShortfall(avgShortfall)
                .recentMarginCalls(convertToMarginCallOverviewDtos(recentMarginCalls))
                .urgentMarginCalls(convertToUrgentMarginCallDtos(urgentCalls))
                .build();
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

    // ==================== RECENT ACTIVITIES ====================

    /**
     * Get recent activities for user
     */
    public List<RecentActivityDto> getRecentActivities(Long userId) {
        log.debug("Generating recent activities for user: {}", userId);

        List<RecentActivityDto> activities = new ArrayList<>();

        // Recent portfolios
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

        // Recent alerts
        List<Alert> recentAlerts = alertRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(5)
                .collect(Collectors.toList());

        for (Alert alert : recentAlerts) {
            activities.add(RecentActivityDto.builder()
                    .activityType("ALERT_TRIGGERED")
                    .description("Alert: " + alert.getTitle())
                    .entityType("ALERT")
                    .entityId(alert.getId())
                    .entityName(alert.getTitle())
                    .status(alert.getIsRead() ? "READ" : "UNREAD")
                    .severity(alert.getSeverity().name())
                    .icon("alert")
                    .actionUrl("/alerts/" + alert.getId())
                    .timestamp(alert.getCreatedAt())
                    .minutesAgo(ChronoUnit.MINUTES.between(alert.getCreatedAt(), LocalDateTime.now()))
                    .build());
        }

        // Recent reports
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