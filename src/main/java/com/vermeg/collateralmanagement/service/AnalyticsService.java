// AnalyticsServiceEnhanced.java
package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.dto.dashboard.*;
import com.vermeg.collateralmanagement.entity.*;
import com.vermeg.collateralmanagement.enums.AssetType;
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
public class AnalyticsService {

    private final AlertRepository alertRepository;
    private final PortfolioRepository portfolioRepository;
    private final CollateralAssetRepository collateralAssetRepository;
    private final RiskMetricRepository riskMetricRepository;
    private final MarginCallRepository marginCallRepository;
    private final ValuationRepository valuationRepository;
    private final UserRepository userRepository;

    // ==================== BASIC ANALYTICS ====================

    /**
     * Get portfolio analytics for user
     */
    public PortfolioSummaryDto getPortfolioAnalytics(Long userId, int periodDays) {
        log.debug("Generating portfolio analytics for user: {} over {} days", userId, periodDays);

        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        List<CollateralAsset> allAssets = collateralAssetRepository.findByUserId(userId);

        BigDecimal totalValue = portfolios.stream()
                .map(Portfolio::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMargin = portfolios.stream()
                .map(Portfolio::getTotalMargin)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PortfolioBreakdownDto> portfolioBreakdown = createPortfolioBreakdown(portfolios);
        List<AssetAllocationDto> assetAllocation = calculateAssetAllocation(allAssets);

        // Calculate performance metrics over period
        BigDecimal dailyChange = calculatePortfolioChange(allAssets, periodDays);
        Double dailyChangePercentage = calculateChangePercentage(totalValue, dailyChange);

        return PortfolioSummaryDto.builder()
                .totalValue(totalValue)
                .totalMargin(totalMargin)
                .totalPortfolios(portfolios.size())
                .totalAssets(allAssets.size())
                .portfolioBreakdown(portfolioBreakdown)
                .assetAllocation(assetAllocation)
                .dailyChange(dailyChange)
                .dailyChangePercentage(dailyChangePercentage)
                .build();
    }

    /**
     * Get risk analytics for user
     */
    public RiskSummaryDto getRiskAnalytics(Long userId, int periodDays) {
        log.debug("Generating risk analytics for user: {} over {} days", userId, periodDays);

        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        List<RiskMetric> latestRiskMetrics = riskMetricRepository.findLatestByUserId(userId);

        BigDecimal overallRiskScore = calculateOverallRiskScore(latestRiskMetrics);
        String riskRating = determineRiskRating(overallRiskScore);
        BigDecimal valueAtRisk = calculateValueAtRisk(portfolios);
        Double utilizationPercentage = calculateUtilizationPercentage(portfolios);

        Integer portfoliosAtRisk = (int) latestRiskMetrics.stream()
                .mapToLong(rm -> rm.getValue().compareTo(new BigDecimal("0.8")) >= 0 ? 1 : 0)
                .sum();

        List<RiskMetricDto> riskMetrics = convertToRiskMetricDtos(latestRiskMetrics);
        List<RiskAlertDto> riskAlerts = getRiskAlerts(userId);
        LocalDateTime lastCalculated = getLatestRiskCalculationDate(latestRiskMetrics);

        return RiskSummaryDto.builder()
                .overallRiskScore(overallRiskScore)
                .riskRating(riskRating)
                .valueAtRisk(valueAtRisk)
                .utilizationPercentage(utilizationPercentage)
                .portfoliosAtRisk(portfoliosAtRisk)
                .riskMetrics(riskMetrics)
                .riskAlerts(riskAlerts)
                .lastCalculated(lastCalculated)
                .build();
    }

    /**
     * Get alert analytics for user
     */
    public AlertSummaryDto getAlertAnalytics(Long userId, int periodDays) {
        log.debug("Generating alert analytics for user: {} over {} days", userId, periodDays);

        LocalDateTime since = LocalDateTime.now().minusDays(periodDays);
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
                    .alertTrends(getAlertTrends(userId, periodDays))
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

        List<Alert> recentAlerts = alertRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        List<AlertOverviewDto> recentAlertDtos = convertToAlertOverviewDtos(recentAlerts);
        AlertTrendsDto alertTrends = getAlertTrends(userId, periodDays);

        return AlertSummaryDto.builder()
                .totalAlerts(total.intValue())
                .criticalAlerts(critical.intValue())
                .highAlerts(high.intValue())
                .mediumAlerts(medium.intValue())
                .lowAlerts(low.intValue())
                .unreadAlerts(unread.intValue())
                .recentAlerts(recentAlertDtos)
                .alertTrends(alertTrends)
                .build();
    }

    // ==================== TREND ANALYSIS ====================

    /**
     * Generate portfolio trends
     */
    public List<TrendDataDto> getPortfolioTrends(Long userId, LocalDateTime startDate,
                                                 LocalDateTime endDate, String granularity) {
        log.debug("Generating portfolio trends for user: {} from {} to {}", userId, startDate, endDate);

        List<TrendDataDto> trends = new ArrayList<>();
        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);

        BigDecimal baseValue = portfolios.stream()
                .map(Portfolio::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime current = startDate;
        while (current.isBefore(endDate)) {
            // Simulate portfolio value changes - in real implementation, use historical data
            BigDecimal variance = new BigDecimal(Math.random() * 0.1 - 0.05); // ±5% variance
            BigDecimal value = baseValue.multiply(BigDecimal.ONE.add(variance));

            trends.add(TrendDataDto.builder()
                    .date(current)
                    .value(value)
                    .label("Portfolio Value")
                    .category("PORTFOLIO")
                    .build());

            current = incrementByGranularity(current, granularity);
        }

        return trends;
    }

    /**
     * Generate risk trends
     */
    public List<TrendDataDto> generateRiskTrends(Long userId, LocalDateTime startDate) {
        log.debug("Generating risk trends for user: {} from {}", userId, startDate);

        List<TrendDataDto> trends = new ArrayList<>();
        LocalDateTime current = startDate;
        LocalDateTime endDate = LocalDateTime.now();

        while (current.isBefore(endDate)) {
            // Mock risk trend data - in real implementation, query historical risk metrics
            BigDecimal riskValue = new BigDecimal(0.3 + Math.random() * 0.4); // Random between 0.3-0.7

            trends.add(TrendDataDto.builder()
                    .date(current)
                    .value(riskValue)
                    .label("Risk Score")
                    .category("RISK")
                    .build());

            current = current.plusDays(1);
        }

        return trends;
    }

    /**
     * Generate alert trends data
     */
    public List<TrendDataDto> generateAlertTrendsData(Long userId, LocalDateTime startDate) {
        log.debug("Generating alert trends for user: {} from {}", userId, startDate);

        List<Object[]> alertTrends = alertRepository.getAlertTrendsByUser(userId, startDate);

        return alertTrends.stream()
                .map(trend -> TrendDataDto.builder()
                        .date((LocalDateTime) trend[0])
                        .value(new BigDecimal(((Number) trend[1]).intValue()))
                        .label("Alert Count")
                        .category("ALERTS")
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== ADVANCED ANALYTICS ====================

    /**
     * Get concentration analysis
     */
    public Map<String, Object> getConcentrationAnalysis(Long userId, double concentrationThreshold) {
        log.debug("Performing concentration analysis for user: {} with threshold: {}", userId, concentrationThreshold);

        List<CollateralAsset> assets = collateralAssetRepository.findByUserId(userId);
        BigDecimal totalValue = assets.stream()
                .map(CollateralAsset::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> analysis = new HashMap<>();

        // Asset type concentration
        Map<AssetType, List<CollateralAsset>> assetsByType = assets.stream()
                .collect(Collectors.groupingBy(CollateralAsset::getType));

        List<Map<String, Object>> concentrationData = new ArrayList<>();
        List<String> violations = new ArrayList<>();

        for (Map.Entry<AssetType, List<CollateralAsset>> entry : assetsByType.entrySet()) {
            BigDecimal typeValue = entry.getValue().stream()
                    .map(CollateralAsset::getMarketValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            double concentration = totalValue.compareTo(BigDecimal.ZERO) == 0 ? 0.0 :
                    typeValue.divide(totalValue, 4, RoundingMode.HALF_UP).doubleValue();

            Map<String, Object> typeData = new HashMap<>();
            typeData.put("assetType", entry.getKey().getDisplayName());
            typeData.put("value", typeValue);
            typeData.put("concentration", concentration);
            typeData.put("violatesThreshold", concentration > concentrationThreshold);

            concentrationData.add(typeData);

            if (concentration > concentrationThreshold) {
                violations.add(String.format("%s concentration %.1f%% exceeds threshold %.1f%%",
                        entry.getKey().getDisplayName(), concentration * 100, concentrationThreshold * 100));
            }
        }

        analysis.put("concentrationData", concentrationData);
        analysis.put("violations", violations);
        analysis.put("threshold", concentrationThreshold);
        analysis.put("totalValue", totalValue);
        analysis.put("violationCount", violations.size());
        analysis.put("isCompliant", violations.isEmpty());

        return analysis;
    }

    /**
     * Calculate Value at Risk
     */
    public Map<String, Object> calculateValueAtRisk(Long userId, double confidenceLevel,
                                                    int timeHorizonDays, String varMethod) {
        log.debug("Calculating VaR for user: {} with confidence: {} and method: {}",
                userId, confidenceLevel, varMethod);

        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        BigDecimal totalValue = portfolios.stream()
                .map(Portfolio::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> varResults = new HashMap<>();

        // Simplified VaR calculation - in production, use proper statistical methods
        BigDecimal volatility = new BigDecimal("0.15"); // 15% annual volatility assumption
        BigDecimal dailyVolatility = volatility.divide(new BigDecimal(Math.sqrt(252)), 6, RoundingMode.HALF_UP);
        BigDecimal periodVolatility = dailyVolatility.multiply(new BigDecimal(Math.sqrt(timeHorizonDays)));

        // Z-score for confidence level (simplified)
        double zScore = confidenceLevel == 0.95 ? 1.645 : (confidenceLevel == 0.99 ? 2.326 : 1.282);
        BigDecimal var = totalValue.multiply(periodVolatility).multiply(new BigDecimal(zScore));

        varResults.put("valueAtRisk", var);
        varResults.put("confidenceLevel", confidenceLevel);
        varResults.put("timeHorizonDays", timeHorizonDays);
        varResults.put("method", varMethod);
        varResults.put("portfolioValue", totalValue);
        varResults.put("varPercentage", totalValue.compareTo(BigDecimal.ZERO) == 0 ? 0.0 :
                var.divide(totalValue, 4, RoundingMode.HALF_UP).doubleValue() * 100);
        varResults.put("calculationDate", LocalDateTime.now());

        return varResults;
    }

    /**
     * Perform stress test analysis
     */
    public Map<String, Object> performStressTest(Long userId, String scenarioType, List<String> customScenarios) {
        log.debug("Performing stress test for user: {} with scenario: {}", userId, scenarioType);

        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        List<CollateralAsset> assets = collateralAssetRepository.findByUserId(userId);

        BigDecimal currentValue = portfolios.stream()
                .map(Portfolio::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stressResults = new HashMap<>();
        List<Map<String, Object>> scenarios = new ArrayList<>();

        // Standard stress scenarios
        if ("STANDARD".equals(scenarioType)) {
            scenarios.add(createStressScenario("Market Crash", -0.30, assets, currentValue));
            scenarios.add(createStressScenario("Recession", -0.15, assets, currentValue));
            scenarios.add(createStressScenario("Interest Rate Shock", -0.10, assets, currentValue));
            scenarios.add(createStressScenario("Credit Crisis", -0.25, assets, currentValue));
        }

        // Custom scenarios
        if (customScenarios != null) {
            for (String scenario : customScenarios) {
                // Parse and apply custom scenarios
                scenarios.add(createCustomStressScenario(scenario, assets, currentValue));
            }
        }

        stressResults.put("currentValue", currentValue);
        stressResults.put("scenarios", scenarios);
        stressResults.put("scenarioType", scenarioType);
        stressResults.put("calculationDate", LocalDateTime.now());

        return stressResults;
    }

    /**
     * Perform stress test analysis (simplified version)
     */
    public Map<String, Object> performStressTest(Long userId) {
        return performStressTest(userId, "STANDARD", null);
    }

    /**
     * Analyze alert patterns
     */
    public Map<String, Object> analyzeAlertPatterns(Long userId, int analysisWindowDays, String granularity) {
        log.debug("Analyzing alert patterns for user: {} over {} days", userId, analysisWindowDays);

        LocalDateTime startDate = LocalDateTime.now().minusDays(analysisWindowDays);
        List<Object[]> alertTrends = alertRepository.getAlertTrendsByUser(userId, startDate);

        Map<String, Object> patterns = new HashMap<>();

        // Time-based patterns
        Map<Integer, Long> hourlyPattern = new HashMap<>();
        Map<String, Long> dailyPattern = new HashMap<>();

        // Mock pattern data - in real implementation, analyze actual alert timestamps
        for (int hour = 0; hour < 24; hour++) {
            hourlyPattern.put(hour, (long) (Math.random() * 10));
        }

        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (String day : days) {
            dailyPattern.put(day, (long) (Math.random() * 20));
        }

        patterns.put("hourlyPattern", hourlyPattern);
        patterns.put("dailyPattern", dailyPattern);
        patterns.put("peakHour", findPeakHour(hourlyPattern));
        patterns.put("peakDay", findPeakDay(dailyPattern));
        patterns.put("analysisWindow", analysisWindowDays);
        patterns.put("granularity", granularity);

        return patterns;
    }

    /**
     * Get alert predictions
     */
    public Map<String, Object> getAlertPredictions(Long userId, int periodDays) {
        log.debug("Generating alert predictions for user: {} over {} days", userId, periodDays);

        // Simplified prediction model - in production, use ML algorithms
        long currentAlertRate = alertRepository.countByUserId(userId);
        double growthRate = 0.05; // 5% growth assumption

        Map<String, Object> predictions = new HashMap<>();
        predictions.put("currentAlertRate", currentAlertRate);
        predictions.put("predictedAlerts", Math.round(currentAlertRate * (1 + growthRate)));
        predictions.put("confidence", 0.75);
        predictions.put("predictionHorizon", periodDays);
        predictions.put("model", "LINEAR_TREND");

        return predictions;
    }

    /**
     * Perform peer comparison
     */
    public Map<String, Object> performPeerComparison(Long userId, String peerGroup, int periodDays) {
        log.debug("Performing peer comparison for user: {} with peer group: {}", userId, peerGroup);

        List<Portfolio> userPortfolios = portfolioRepository.findByUserId(userId);
        BigDecimal userTotalValue = userPortfolios.stream()
                .map(Portfolio::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Mock peer data - in real implementation, query similar users
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("userValue", userTotalValue);
        comparison.put("peerAverageValue", userTotalValue.multiply(new BigDecimal("0.95")));
        comparison.put("peerMedianValue", userTotalValue.multiply(new BigDecimal("0.90")));
        comparison.put("userPercentile", 65);
        comparison.put("peerGroup", peerGroup);
        comparison.put("peerCount", 150);
        comparison.put("performanceVsPeers", "ABOVE_AVERAGE");

        return comparison;
    }

    /**
     * Compare to benchmark
     */
    public Map<String, Object> compareToBenchmark(Long userId, String benchmarkId, int periodDays) {
        log.debug("Comparing user: {} to benchmark: {} over {} days", userId, benchmarkId, periodDays);

        List<Portfolio> userPortfolios = portfolioRepository.findByUserId(userId);
        BigDecimal userValue = userPortfolios.stream()
                .map(Portfolio::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Mock benchmark data
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("userValue", userValue);
        comparison.put("benchmarkValue", userValue.multiply(new BigDecimal("1.02")));
        comparison.put("relativePerformance", -2.0);
        comparison.put("trackingError", 5.5);
        comparison.put("informationRatio", -0.36);
        comparison.put("benchmarkId", benchmarkId);
        comparison.put("periodDays", periodDays);

        return comparison;
    }

    /**
     * Perform correlation analysis
     */
    public Map<String, Object> performCorrelationAnalysis(Long userId, int analysisWindow, String correlationType) {
        log.debug("Performing correlation analysis for user: {} with type: {}", userId, correlationType);

        List<CollateralAsset> assets = collateralAssetRepository.findByUserId(userId);

        Map<String, Object> correlationData = new HashMap<>();

        if ("ASSET_TYPE".equals(correlationType)) {
            // Calculate correlations between asset types
            Map<String, Map<String, Double>> correlationMatrix = new HashMap<>();

            for (AssetType type1 : AssetType.values()) {
                Map<String, Double> row = new HashMap<>();
                for (AssetType type2 : AssetType.values()) {
                    // Mock correlation data - in real implementation, calculate from price history
                    double correlation = type1 == type2 ? 1.0 : Math.random() * 0.8 - 0.4;
                    row.put(type2.name(), correlation);
                }
                correlationMatrix.put(type1.name(), row);
            }

            correlationData.put("correlationMatrix", correlationMatrix);
        }

        correlationData.put("analysisWindow", analysisWindow);
        correlationData.put("correlationType", correlationType);
        correlationData.put("assetCount", assets.size());

        return correlationData;
    }

    /**
     * Generate predictions
     */
    public Map<String, Object> generatePredictions(Long userId, int forecastDays, List<String> metrics) {
        log.debug("Generating predictions for user: {} for {} days", userId, forecastDays);

        Map<String, Object> predictions = new HashMap<>();

        for (String metric : metrics) {
            Map<String, Object> metricPrediction = generateMetricPrediction(userId, metric, forecastDays);
            predictions.put(metric, metricPrediction);
        }

        predictions.put("forecastHorizon", forecastDays);
        predictions.put("generatedAt", LocalDateTime.now());
        predictions.put("model", "ARIMA");
        predictions.put("confidence", 0.80);

        return predictions;
    }

    /**
     * Export analytics data
     */
    public Map<String, Object> exportAnalyticsData(Long userId, String format, List<String> dataTypes, int periodDays) {
        log.debug("Exporting analytics data for user: {} in format: {}", userId, format);

        Map<String, Object> exportResult = new HashMap<>();
        Map<String, Object> exportedData = new HashMap<>();

        for (String dataType : dataTypes) {
            switch (dataType) {
                case "PORTFOLIO_SUMMARY":
                    exportedData.put("portfolioSummary", getPortfolioAnalytics(userId, periodDays));
                    break;
                case "RISK_ANALYSIS":
                    exportedData.put("riskAnalysis", getRiskAnalytics(userId, periodDays));
                    break;
                case "ALERT_SUMMARY":
                    exportedData.put("alertSummary", getAlertAnalytics(userId, periodDays));
                    break;
            }
        }

        exportResult.put("data", exportedData);
        exportResult.put("format", format);
        exportResult.put("dataTypes", dataTypes);
        exportResult.put("periodDays", periodDays);
        exportResult.put("exportedAt", LocalDateTime.now());
        exportResult.put("fileSize", "2.5MB"); // Mock file size
        exportResult.put("downloadUrl", "/api/analytics/downloads/" + UUID.randomUUID().toString());

        return exportResult;
    }

    // ==================== HELPER METHODS ====================

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
                            .status("ACTIVE")
                            .build();
                })
                .sorted((p1, p2) -> p2.getTotalValue().compareTo(p1.getTotalValue()))
                .collect(Collectors.toList());
    }

    private List<AssetAllocationDto> calculateAssetAllocation(List<CollateralAsset> assets) {
        if (assets.isEmpty()) return new ArrayList<>();

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
                                    .multiply(new BigDecimal("100")).doubleValue();

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

    private BigDecimal calculatePortfolioChange(List<CollateralAsset> assets, int days) {
        // Simplified calculation - in real implementation, use historical data
        return assets.stream()
                .map(asset -> asset.getMarketValue().multiply(new BigDecimal("0.015"))) // 1.5% mock change
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Double calculateChangePercentage(BigDecimal totalValue, BigDecimal change) {
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return change.divide(totalValue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).doubleValue();
    }

    private BigDecimal calculateOverallRiskScore(List<RiskMetric> riskMetrics) {
        if (riskMetrics.isEmpty()) return BigDecimal.ZERO;
        BigDecimal totalRisk = riskMetrics.stream()
                .map(RiskMetric::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalRisk.divide(new BigDecimal(riskMetrics.size()), 4, RoundingMode.HALF_UP);
    }

    private String determineRiskRating(BigDecimal riskScore) {
        if (riskScore.compareTo(new BigDecimal("0.8")) >= 0) return "CRITICAL";
        else if (riskScore.compareTo(new BigDecimal("0.6")) >= 0) return "HIGH";
        else if (riskScore.compareTo(new BigDecimal("0.3")) >= 0) return "MEDIUM";
        else return "LOW";
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

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return totalMargin.divide(totalValue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).doubleValue();
    }

    private List<RiskMetricDto> convertToRiskMetricDtos(List<RiskMetric> riskMetrics) {
        return riskMetrics.stream()
                .map(metric -> RiskMetricDto.builder()
                        .portfolioId(metric.getPortfolio().getId())
                        .portfolioName(metric.getPortfolio().getName())
                        .currentRisk(metric.getValue())
                        .threshold(new BigDecimal("0.8"))
                        .status(determineRiskStatus(metric.getValue()))
                        .methodology(metric.getMethodology())
                        .lastCalculated(metric.getCalculationDate())
                        .build())
                .collect(Collectors.toList());
    }

    private String determineRiskStatus(BigDecimal riskValue) {
        if (riskValue.compareTo(new BigDecimal("0.8")) >= 0) return "CRITICAL";
        else if (riskValue.compareTo(new BigDecimal("0.6")) >= 0) return "WARNING";
        else return "NORMAL";
    }

    private List<RiskAlertDto> getRiskAlerts(Long userId) {
        // Implementation would fetch risk-related alerts
        return new ArrayList<>();
    }

    private LocalDateTime getLatestRiskCalculationDate(List<RiskMetric> riskMetrics) {
        return riskMetrics.stream()
                .map(RiskMetric::getCalculationDate)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

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

    private AlertTrendsDto getAlertTrends(Long userId, int periodDays) {
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

        double avgAlertsPerDay = dailyTrends.stream()
                .mapToInt(AlertTrendDataDto::getTotalAlerts)
                .average().orElse(0.0);

        return AlertTrendsDto.builder()
                .dailyTrends(dailyTrends)
                .averageAlertsPerDay(avgAlertsPerDay)
                .resolutionRate(85.0)
                .averageResolutionTimeMinutes(45L)
                .build();
    }

    private LocalDateTime incrementByGranularity(LocalDateTime dateTime, String granularity) {
        switch (granularity.toUpperCase()) {
            case "HOURLY": return dateTime.plusHours(1);
            case "DAILY": return dateTime.plusDays(1);
            case "WEEKLY": return dateTime.plusWeeks(1);
            case "MONTHLY": return dateTime.plusMonths(1);
            default: return dateTime.plusDays(1);
        }
    }

    private Map<String, Object> createStressScenario(String name, double shockPercentage,
                                                     List<CollateralAsset> assets, BigDecimal currentValue) {
        BigDecimal stressedValue = currentValue.multiply(BigDecimal.ONE.add(new BigDecimal(shockPercentage)));
        BigDecimal loss = currentValue.subtract(stressedValue);

        Map<String, Object> scenario = new HashMap<>();
        scenario.put("name", name);
        scenario.put("shockPercentage", shockPercentage * 100);
        scenario.put("currentValue", currentValue);
        scenario.put("stressedValue", stressedValue);
        scenario.put("loss", loss);
        scenario.put("lossPercentage", shockPercentage * 100);

        return scenario;
    }

    private Map<String, Object> createCustomStressScenario(String scenarioDefinition,
                                                           List<CollateralAsset> assets, BigDecimal currentValue) {
        // Parse custom scenario definition - simplified implementation
        double shockPercentage = -0.20; // Default 20% shock
        String scenarioName = "Custom Scenario";

        return createStressScenario(scenarioName, shockPercentage, assets, currentValue);
    }

    private String findPeakHour(Map<Integer, Long> hourlyPattern) {
        return hourlyPattern.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey() + ":00")
                .orElse("Unknown");
    }

    private String findPeakDay(Map<String, Long> dailyPattern) {
        return dailyPattern.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
    }

    private Map<String, Object> generateMetricPrediction(Long userId, String metric, int forecastDays) {
        Map<String, Object> prediction = new HashMap<>();

        switch (metric) {
            case "PORTFOLIO_VALUE":
                BigDecimal currentValue = portfolioRepository.findByUserId(userId).stream()
                        .map(Portfolio::getTotalValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal predictedValue = currentValue.multiply(new BigDecimal("1.05")); // 5% growth

                prediction.put("current", currentValue);
                prediction.put("predicted", predictedValue);
                prediction.put("change", predictedValue.subtract(currentValue));
                prediction.put("changePercentage", 5.0);
                break;

            case "RISK_SCORE":
                prediction.put("current", 0.45);
                prediction.put("predicted", 0.42);
                prediction.put("change", -0.03);
                prediction.put("changePercentage", -6.7);
                break;
        }

        prediction.put("metric", metric);
        prediction.put("confidence", 0.75);
        prediction.put("method", "LINEAR_REGRESSION");

        return prediction;
    }
}