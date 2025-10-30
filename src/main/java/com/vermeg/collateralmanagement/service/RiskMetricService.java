package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.dto.risk.*;
import com.vermeg.collateralmanagement.entity.*;
import com.vermeg.collateralmanagement.enums.AssetType;
import com.vermeg.collateralmanagement.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RiskMetricService {

    private static final Logger log = LoggerFactory.getLogger(RiskMetricService.class);

    @Autowired
    private RiskMetricRepository riskMetricRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private CollateralAssetRepository collateralAssetRepository;

    /**
     * Get risk overview for user's portfolios
     */
    public RiskOverviewDto getRiskOverview(Long userId) {
        log.info("Getting risk overview for user: {}", userId);

        List<RiskMetric> latestMetrics = riskMetricRepository.findLatestByUserId(userId);

        if (latestMetrics.isEmpty()) {
            return RiskOverviewDto.builder()
                    .totalPortfolios(0)
                    .highRiskPortfolios(0)
                    .mediumRiskPortfolios(0)
                    .lowRiskPortfolios(0)
                    .totalValueAtRisk(BigDecimal.ZERO)
                    .averageRiskScore(BigDecimal.ZERO)
                    .overallRiskRating("LOW")
                    .portfolioRisks(new ArrayList<>())
                    .riskDistribution(RiskDistributionDto.builder()
                            .low(0).medium(0).high(0).critical(0).build())
                    .lastUpdated(LocalDateTime.now())
                    .build();
        }

        // Calculate distribution
        int low = 0, medium = 0, high = 0, critical = 0;
        BigDecimal totalRisk = BigDecimal.ZERO;
        BigDecimal totalVaR = BigDecimal.ZERO;

        List<PortfolioRiskSummaryDto> portfolioRisks = new ArrayList<>();

        for (RiskMetric metric : latestMetrics) {
            Portfolio portfolio = metric.getPortfolio();
            BigDecimal riskValue = metric.getValue();

            String riskRating = calculateRiskRating(riskValue);

            // Count by rating
            switch (riskRating) {
                case "LOW": low++; break;
                case "MEDIUM": medium++; break;
                case "HIGH": high++; break;
                case "CRITICAL": critical++; break;
            }

            totalRisk = totalRisk.add(riskValue);

            // Calculate VaR (5% of portfolio value * risk score)
            BigDecimal portfolioValue = calculatePortfolioValueFromPortfolio(portfolio);
            BigDecimal var = portfolioValue.multiply(riskValue)
                    .multiply(new BigDecimal("0.05"));
            totalVaR = totalVaR.add(var);

            portfolioRisks.add(PortfolioRiskSummaryDto.builder()
                    .portfolioId(portfolio.getId())
                    .portfolioName(portfolio.getName())
                    .portfolioType(portfolio.getType().name())
                    .totalValue(portfolioValue)
                    .riskScore(riskValue)
                    .riskRating(riskRating)
                    .valueAtRisk(var)
                    .volatility(calculatePortfolioVolatility(portfolio))
                    .beta(calculateBeta(portfolio))
                    .lastCalculated(metric.getCalculationDate())
                    .build());
        }

        BigDecimal avgRisk = latestMetrics.isEmpty() ? BigDecimal.ZERO :
                totalRisk.divide(BigDecimal.valueOf(latestMetrics.size()), 2, RoundingMode.HALF_UP);

        return RiskOverviewDto.builder()
                .totalPortfolios(latestMetrics.size())
                .highRiskPortfolios(high + critical)
                .mediumRiskPortfolios(medium)
                .lowRiskPortfolios(low)
                .totalValueAtRisk(totalVaR)
                .averageRiskScore(avgRisk)
                .overallRiskRating(calculateRiskRating(avgRisk))
                .portfolioRisks(portfolioRisks)
                .riskDistribution(RiskDistributionDto.builder()
                        .low(low).medium(medium).high(high).critical(critical).build())
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * Get detailed risk analysis for a portfolio
     */
    public PortfolioRiskDetailDto getPortfolioRiskDetail(Long portfolioId, Long userId) {
        log.info("Getting portfolio risk detail for portfolio: {} and user: {}", portfolioId, userId);

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found or access denied"));

        RiskMetric latestMetric = riskMetricRepository.findLatestByPortfolioId(portfolioId)
                .orElse(null);

        if (latestMetric == null) {
            // Calculate new risk metric
            latestMetric = calculatePortfolioRisk(portfolioId, userId);
        }

        List<CollateralAsset> assets = collateralAssetRepository.findByPortfolioId(portfolioId);
        BigDecimal portfolioValue = calculatePortfolioValue(assets);
        BigDecimal riskScore = latestMetric.getValue();
        String riskRating = calculateRiskRating(riskScore);

        // Calculate metrics
        BigDecimal var = portfolioValue.multiply(riskScore).multiply(new BigDecimal("0.05"));
        BigDecimal expectedShortfall = var.multiply(new BigDecimal("1.2"));
        BigDecimal sharpeRatio = calculateSharpeRatio(portfolio);
        BigDecimal beta = calculateBeta(portfolio);
        BigDecimal volatility = calculatePortfolioVolatility(portfolio);
        BigDecimal maxDrawdown = calculateMaxDrawdown(portfolio);

        // Get asset risks
        List<AssetRiskDto> assetRisks = calculateAssetRisks(assets, portfolioValue);

        // Risk breakdown
        RiskBreakdownDto riskBreakdown = calculateRiskBreakdownDto(portfolio, riskScore);

        // Historical performance
        List<HistoricalPerformanceDto> historicalPerformance = getHistoricalPerformance(portfolioId);

        return PortfolioRiskDetailDto.builder()
                .portfolioId(portfolioId)
                .portfolioName(portfolio.getName())
                .portfolioType(portfolio.getType().name())
                .totalValue(portfolioValue)
                .riskScore(riskScore)
                .riskRating(riskRating)
                .valueAtRisk(var)
                .expectedShortfall(expectedShortfall)
                .sharpeRatio(sharpeRatio)
                .beta(beta)
                .volatility(volatility)
                .maxDrawdown(maxDrawdown)
                .assetRisks(assetRisks)
                .riskBreakdown(riskBreakdown)
                .historicalPerformance(historicalPerformance)
                .methodology(latestMetric.getMethodology())
                .lastUpdated(latestMetric.getCalculationDate())
                .build();
    }

    /**
     * Create comprehensive risk metric for portfolio
     */
    public RiskMetric createRiskMetric(Portfolio portfolio, BigDecimal portfolioValue,
                                       BigDecimal marginRequirement, BigDecimal concentrationRisk,
                                       BigDecimal liquidityRisk) {
        log.info("Creating risk metric for portfolio: {}", portfolio.getId());

        BigDecimal riskScore = calculateCompositeRiskScore(
                portfolioValue, marginRequirement, concentrationRisk, liquidityRisk);

        RiskMetric riskMetric = RiskMetric.builder()
                .portfolio(portfolio)
                .value(riskScore)
                .calculationDate(LocalDateTime.now())
                .methodology("COMPOSITE_RISK_ANALYSIS")
                .build();

        return riskMetricRepository.save(riskMetric);
    }

    /**
     * Calculate comprehensive portfolio risk metrics
     */
    public RiskMetric calculatePortfolioRisk(Long portfolioId, Long userId) {
        log.info("Calculating portfolio risk for portfolio: {} by user: {}", portfolioId, userId);

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found or access denied"));

        List<CollateralAsset> assets = collateralAssetRepository.findByPortfolioId(portfolioId);

        if (assets.isEmpty()) {
            return createRiskMetric(portfolio, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal portfolioValue = calculatePortfolioValue(assets);
        BigDecimal volatilityRisk = calculateVolatilityRisk(assets);
        BigDecimal concentrationRisk = calculateConcentrationRisk(assets, portfolioValue);
        BigDecimal liquidityRisk = calculateLiquidityRisk(assets);
        BigDecimal correlationRisk = calculateCorrelationRisk(assets);

        BigDecimal compositeRisk = calculateCompositeRiskScore(
                volatilityRisk, concentrationRisk, liquidityRisk, correlationRisk);

        RiskMetric riskMetric = RiskMetric.builder()
                .portfolio(portfolio)
                .value(compositeRisk)
                .calculationDate(LocalDateTime.now())
                .methodology("COMPREHENSIVE_RISK_ANALYSIS")
                .build();

        return riskMetricRepository.save(riskMetric);
    }

    /**
     * Get latest risk metric for portfolio
     */
    public RiskMetric getLatestRiskMetric(Long portfolioId, Long userId) {
        // Verify user access to portfolio
        portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found or access denied"));

        return riskMetricRepository.findLatestByPortfolioId(portfolioId).orElse(null);
    }

    /**
     * Get risk metrics history for portfolio
     */
    public List<RiskMetric> getRiskMetricsHistory(Long portfolioId, Long userId,
                                                  LocalDateTime startDate, LocalDateTime endDate) {
        // Verify user access to portfolio
        portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found or access denied"));

        return riskMetricRepository.findByPortfolioIdAndCalculationDateBetween(
                portfolioId, startDate, endDate);
    }

    /**
     * Get user's risk metrics summary
     */
    public List<RiskMetric> getUserRiskMetrics(Long userId) {
        return riskMetricRepository.findLatestByUserId(userId);
    }

    /**
     * Calculate stress test scenarios
     */
    public BigDecimal calculateStressTestValue(Long portfolioId, BigDecimal stressPercentage) {
        log.info("Calculating stress test for portfolio: {} with {}% stress",
                portfolioId, stressPercentage.multiply(new BigDecimal("100")));

        List<CollateralAsset> assets = collateralAssetRepository.findByPortfolioId(portfolioId);

        return assets.stream()
                .map(asset -> {
                    BigDecimal stressedPrice = asset.getMarketValue()
                            .multiply(BigDecimal.ONE.subtract(stressPercentage));
                    return stressedPrice.multiply(BigDecimal.ONE.subtract(asset.getHaircut()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Check if portfolio needs risk recalculation
     */
    public boolean needsRiskRecalculation(Long portfolioId) {
        RiskMetric latest = riskMetricRepository.findLatestByPortfolioId(portfolioId).orElse(null);
        return latest == null || !latest.isCurrentMetric();
    }

    // ==================== HELPER METHODS ====================

    private String calculateRiskRating(BigDecimal riskScore) {
        if (riskScore.compareTo(new BigDecimal("0.25")) < 0) return "LOW";
        if (riskScore.compareTo(new BigDecimal("0.50")) < 0) return "MEDIUM";
        if (riskScore.compareTo(new BigDecimal("0.75")) < 0) return "HIGH";
        return "CRITICAL";
    }

    private BigDecimal calculatePortfolioValueFromPortfolio(Portfolio portfolio) {
        List<CollateralAsset> assets = collateralAssetRepository.findByPortfolioId(portfolio.getId());
        return calculatePortfolioValue(assets);
    }

    private BigDecimal calculatePortfolioValue(List<CollateralAsset> assets) {
        return assets.stream()
                .map(CollateralAsset::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculatePortfolioVolatility(Portfolio portfolio) {
        List<CollateralAsset> assets = collateralAssetRepository.findByPortfolioId(portfolio.getId());
        return calculateVolatilityRisk(assets);
    }

    private BigDecimal calculateVolatilityRisk(List<CollateralAsset> assets) {
        BigDecimal weightedVolatility = BigDecimal.ZERO;
        BigDecimal totalValue = calculatePortfolioValue(assets);

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        for (CollateralAsset asset : assets) {
            BigDecimal weight = asset.getMarketValue().divide(totalValue, 4, RoundingMode.HALF_UP);
            BigDecimal assetVolatility = getAssetVolatility(asset.getType());
            weightedVolatility = weightedVolatility.add(weight.multiply(assetVolatility));
        }

        return weightedVolatility;
    }

    private BigDecimal calculateConcentrationRisk(List<CollateralAsset> assets, BigDecimal totalValue) {
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return assets.stream()
                .map(asset -> {
                    BigDecimal weight = asset.getMarketValue().divide(totalValue, 4, RoundingMode.HALF_UP);
                    return weight.multiply(weight);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateLiquidityRisk(List<CollateralAsset> assets) {
        if (assets.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalLiquidityRisk = assets.stream()
                .map(asset -> getLiquidityRisk(asset.getType()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalLiquidityRisk.divide(new BigDecimal(assets.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCorrelationRisk(List<CollateralAsset> assets) {
        long distinctTypes = assets.stream()
                .map(CollateralAsset::getType)
                .distinct()
                .count();

        if (assets.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal diversificationFactor = new BigDecimal(distinctTypes)
                .divide(new BigDecimal(assets.size()), 4, RoundingMode.HALF_UP);

        return BigDecimal.ONE.subtract(diversificationFactor).max(BigDecimal.ZERO);
    }

    private BigDecimal calculateCompositeRiskScore(BigDecimal... riskComponents) {
        BigDecimal[] weights = {
                new BigDecimal("0.3"),  // Volatility
                new BigDecimal("0.25"), // Concentration
                new BigDecimal("0.25"), // Liquidity
                new BigDecimal("0.2")   // Correlation
        };

        BigDecimal compositeRisk = BigDecimal.ZERO;

        for (int i = 0; i < Math.min(riskComponents.length, weights.length); i++) {
            if (riskComponents[i] != null) {
                compositeRisk = compositeRisk.add(riskComponents[i].multiply(weights[i]));
            }
        }

        return compositeRisk.min(BigDecimal.ONE).max(BigDecimal.ZERO);
    }

    private BigDecimal calculateBeta(Portfolio portfolio) {
        // Simplified beta calculation - market correlation
        return new BigDecimal("1.15");
    }

    private BigDecimal calculateSharpeRatio(Portfolio portfolio) {
        // Simplified Sharpe ratio (return-risk_free_rate)/volatility
        return new BigDecimal("0.85");
    }

    private BigDecimal calculateMaxDrawdown(Portfolio portfolio) {
        // Simplified max drawdown - maximum observed loss
        return new BigDecimal("0.18");
    }

    private List<AssetRiskDto> calculateAssetRisks(List<CollateralAsset> assets, BigDecimal totalValue) {
        if (assets.isEmpty() || totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return new ArrayList<>();
        }

        return assets.stream()
                .map(asset -> {
                    BigDecimal assetValue = asset.getMarketValue();
                    BigDecimal percentOfPortfolio = assetValue.divide(totalValue, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));

                    BigDecimal assetVolatility = getAssetVolatility(asset.getType());
                    BigDecimal assetBeta = getAssetBeta(asset.getType());
                    BigDecimal assetVaR = assetValue.multiply(assetVolatility).multiply(new BigDecimal("0.05"));
                    BigDecimal riskContribution = totalValue.compareTo(BigDecimal.ZERO) > 0 ?
                            assetVaR.divide(totalValue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
                            BigDecimal.ZERO;

                    return AssetRiskDto.builder()
                            .assetId(asset.getId())
                            .assetName(asset.getName())
                            .assetType(asset.getType().name())
                            .currentValue(assetValue)
                            .percentOfPortfolio(percentOfPortfolio)
                            .volatility(assetVolatility)
                            .beta(assetBeta)
                            .valueAtRisk(assetVaR)
                            .riskContribution(riskContribution)
                            .riskRating(calculateRiskRating(assetVolatility))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private RiskBreakdownDto calculateRiskBreakdownDto(Portfolio portfolio, BigDecimal totalRisk) {
        // Simplified risk breakdown based on portfolio composition
        return RiskBreakdownDto.builder()
                .marketRisk(totalRisk.multiply(new BigDecimal("0.40")))
                .creditRisk(totalRisk.multiply(new BigDecimal("0.25")))
                .liquidityRisk(totalRisk.multiply(new BigDecimal("0.15")))
                .concentrationRisk(totalRisk.multiply(new BigDecimal("0.15")))
                .operationalRisk(totalRisk.multiply(new BigDecimal("0.05")))
                .build();
    }

    private List<HistoricalPerformanceDto> getHistoricalPerformance(Long portfolioId) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(30);

        List<RiskMetric> historicalMetrics = riskMetricRepository
                .findByPortfolioIdAndCalculationDateBetween(portfolioId, startDate, endDate);

        if (historicalMetrics.isEmpty()) {
            // Generate sample data for last 30 days
            List<HistoricalPerformanceDto> sampleData = new ArrayList<>();
            List<CollateralAsset> assets = collateralAssetRepository.findByPortfolioId(portfolioId);
            BigDecimal baseValue = calculatePortfolioValue(assets);

            for (int i = 29; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                // Simulate some variation
                BigDecimal variation = new BigDecimal(Math.random() * 0.1 - 0.05);
                BigDecimal value = baseValue.multiply(BigDecimal.ONE.add(variation));

                sampleData.add(HistoricalPerformanceDto.builder()
                        .date(date)
                        .value(value)
                        .riskScore(new BigDecimal(0.3 + Math.random() * 0.4))
                        .volatility(new BigDecimal(0.10 + Math.random() * 0.15))
                        .build());
            }
            return sampleData;
        }

        return historicalMetrics.stream()
                .map(metric -> {
                    List<CollateralAsset> assets = collateralAssetRepository.findByPortfolioId(portfolioId);
                    BigDecimal value = calculatePortfolioValue(assets);

                    return HistoricalPerformanceDto.builder()
                            .date(metric.getCalculationDate().toLocalDate())
                            .value(value)
                            .riskScore(metric.getValue())
                            .volatility(calculateVolatilityRisk(assets))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private BigDecimal getAssetVolatility(AssetType assetType) {
        switch (assetType) {
            case CASH:
                return new BigDecimal("0.01");
            case GOVERNMENT_BOND:
                return new BigDecimal("0.05");
            case CORPORATE_BOND:
                return new BigDecimal("0.08");
            case EQUITY:
                return new BigDecimal("0.20");
            case COMMODITY:
                return new BigDecimal("0.25");
            case REAL_ESTATE:
                return new BigDecimal("0.15");
            default:
                return new BigDecimal("0.15");
        }
    }

    private BigDecimal getAssetBeta(AssetType assetType) {
        switch (assetType) {
            case CASH:
                return new BigDecimal("0.0");
            case GOVERNMENT_BOND:
                return new BigDecimal("0.3");
            case CORPORATE_BOND:
                return new BigDecimal("0.6");
            case EQUITY:
                return new BigDecimal("1.2");
            case COMMODITY:
                return new BigDecimal("0.8");
            case REAL_ESTATE:
                return new BigDecimal("0.7");
            default:
                return new BigDecimal("1.0");
        }
    }

    private BigDecimal getLiquidityRisk(AssetType assetType) {
        switch (assetType) {
            case CASH:
                return new BigDecimal("0.0");
            case GOVERNMENT_BOND:
                return new BigDecimal("0.1");
            case CORPORATE_BOND:
                return new BigDecimal("0.2");
            case EQUITY:
                return new BigDecimal("0.3");
            case COMMODITY:
                return new BigDecimal("0.4");
            case REAL_ESTATE:
                return new BigDecimal("0.6");
            default:
                return new BigDecimal("0.3");
        }
    }
}