package com.vermeg.collateralmanagement.service;

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
import java.time.LocalDateTime;
import java.util.List;

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

    // Private calculation methods

    private BigDecimal calculatePortfolioValue(List<CollateralAsset> assets) {
        return assets.stream()
                .map(CollateralAsset::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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