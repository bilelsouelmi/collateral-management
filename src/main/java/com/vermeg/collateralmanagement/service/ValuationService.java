package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.entity.*;
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
import java.util.Map;

@Service
@Transactional
public class ValuationService {

    private static final Logger log = LoggerFactory.getLogger(ValuationService.class);

    @Autowired
    private ValuationRepository valuationRepository;

    @Autowired
    private CollateralAssetRepository collateralAssetRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    /**
     * Create Initial Valuation
     */
    public Valuation createInitialValuation(CollateralAsset asset) {
        log.info("Creating initial valuation for asset: {}", asset.getAssetId());

        Valuation valuation = Valuation.builder()
                .collateralAsset(asset)
                .value(asset.getMarketValue())
                .currency(asset.getCurrency())
                .valuationDate(LocalDateTime.now())
                .build();

        Valuation savedValuation = valuationRepository.save(valuation);
        log.info("Created initial valuation with ID: {}", savedValuation.getId());

        return savedValuation;
    }

    /**
     * Create Market Valuation
     */
    public Valuation createValuation(CollateralAsset asset, BigDecimal newValue) {
        log.info("Creating valuation for asset: {} with value: {}", asset.getAssetId(), newValue);

        // Validate the new value
        if (newValue == null || newValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Market value must be positive");
        }

        Valuation valuation = Valuation.builder()
                .collateralAsset(asset)
                .value(newValue)
                .currency(asset.getCurrency())
                .valuationDate(LocalDateTime.now())
                .build();

        Valuation savedValuation = valuationRepository.save(valuation);

        // Update asset's market value
        asset.setMarketValue(newValue);
        asset.setLastModifiedAt(LocalDateTime.now());
        collateralAssetRepository.save(asset);

        log.info("Created valuation with ID: {}", savedValuation.getId());
        return savedValuation;
    }

    /**
     * Update Asset Market Value
     */
    public CollateralAsset updateAssetMarketValue(Long assetId, BigDecimal newValue, Long userId) {
        log.info("Updating market value for asset: {} to {} by user: {}", assetId, newValue, userId);

        CollateralAsset asset = collateralAssetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetId));

        // Verify user access if asset is in a portfolio
        if (asset.getPortfolio() != null && !asset.getPortfolio().getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied: Asset belongs to another user");
        }

        // Create valuation record
        createValuation(asset, newValue);

        // Update portfolio total value if asset is assigned
        if (asset.getPortfolio() != null) {
            updatePortfolioTotalValue(asset.getPortfolio());
        }

        return asset;
    }

    /**
     * Get Latest Valuation
     */
    public Valuation getLatestValuation(Long assetId) {
        List<Valuation> valuations = valuationRepository.findByCollateralAssetId(assetId);

        return valuations.stream()
                .filter(v -> v.getValuationDate() != null)
                .max((v1, v2) -> v1.getValuationDate().compareTo(v2.getValuationDate()))
                .orElse(null);
    }

    /**
     * Get Valuation History
     */
    public List<Valuation> getValuationHistory(Long assetId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Valuation> allValuations = valuationRepository.findByCollateralAssetId(assetId);

        return allValuations.stream()
                .filter(v -> v.getValuationDate() != null)
                .filter(v -> !v.getValuationDate().isBefore(startDate))
                .filter(v -> !v.getValuationDate().isAfter(endDate))
                .sorted((v1, v2) -> v2.getValuationDate().compareTo(v1.getValuationDate()))
                .toList();
    }

    /**
     * Get Portfolio Current Value
     */
    public BigDecimal getPortfolioCurrentValue(Long portfolioId) {
        List<CollateralAsset> assets = collateralAssetRepository.findByPortfolioId(portfolioId);

        return assets.stream()
                .map(CollateralAsset::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Bulk Update Asset Values
     */
    public void bulkUpdateAssetValues(Map<String, BigDecimal> assetUpdates) {
        log.info("Performing bulk valuation update for {} assets", assetUpdates.size());

        for (Map.Entry<String, BigDecimal> entry : assetUpdates.entrySet()) {
            String assetId = entry.getKey();
            BigDecimal newValue = entry.getValue();

            try {
                CollateralAsset asset = collateralAssetRepository.findByAssetId(assetId)
                        .orElse(null);
                if (asset != null) {
                    createValuation(asset, newValue);
                    log.debug("Updated valuation for asset: {}", assetId);
                } else {
                    log.warn("Asset not found for bulk update: {}", assetId);
                }
            } catch (Exception e) {
                log.error("Failed to update valuation for asset {}: {}", assetId, e.getMessage());
            }
        }

        updateAllPortfolioValues();
        log.info("Bulk valuation update completed");
    }

    /**
     * Simulate Market Data Update
     */
    public void simulateMarketDataUpdate() {
        log.info("Simulating market data update");

        List<CollateralAsset> activeAssets = collateralAssetRepository.findAll()
                .stream()
                .filter(asset -> asset.getStatus() == com.vermeg.collateralmanagement.enums.AssetStatus.ACTIVE)
                .toList();

        for (CollateralAsset asset : activeAssets) {
            BigDecimal currentValue = asset.getMarketValue();
            if (currentValue == null || currentValue.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // Skip assets without valid values
            }

            // Simple 5% volatility simulation
            BigDecimal volatility = new BigDecimal("0.05");
            BigDecimal change = currentValue.multiply(volatility)
                    .multiply(new BigDecimal(Math.random() * 2 - 1)); // Random between -1 and +1

            BigDecimal newValue = currentValue.add(change);

            // Ensure value doesn't go negative
            if (newValue.compareTo(BigDecimal.ZERO) <= 0) {
                newValue = currentValue.multiply(new BigDecimal("0.01")); // 1% of original value
            }

            try {
                createValuation(asset, newValue);
            } catch (Exception e) {
                log.error("Failed to simulate market update for asset {}: {}", asset.getAssetId(), e.getMessage());
            }
        }

        updateAllPortfolioValues();
        log.info("Market data simulation completed");
    }

    /**
     * Find Stale Valuations
     */
    public List<CollateralAsset> findStaleValuations(int hoursThreshold) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(hoursThreshold);

        return collateralAssetRepository.findAll()
                .stream()
                .filter(asset -> {
                    Valuation latest = getLatestValuation(asset.getId());
                    return latest == null || latest.getValuationDate().isBefore(cutoffDate);
                })
                .toList();
    }

    /**
     * Get Asset Value at Date
     */
    public BigDecimal getAssetValueAtDate(Long assetId, LocalDateTime targetDate) {
        List<Valuation> valuations = valuationRepository.findByCollateralAssetId(assetId);

        return valuations.stream()
                .filter(v -> v.getValuationDate() != null)
                .filter(v -> v.getValuationDate().isBefore(targetDate) || v.getValuationDate().isEqual(targetDate))
                .max((v1, v2) -> v1.getValuationDate().compareTo(v2.getValuationDate()))
                .map(Valuation::getValue)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Calculate Asset Volatility (Simple Version)
     */
    public BigDecimal calculateAssetVolatility(Long assetId, int days) {
        List<Valuation> valuations = valuationRepository.findByCollateralAssetId(assetId);

        if (valuations.size() < 2) {
            return BigDecimal.ZERO;
        }

        // Filter recent valuations and sort by date
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Valuation> recentValuations = valuations.stream()
                .filter(v -> v.getValuationDate() != null)
                .filter(v -> !v.getValuationDate().isBefore(startDate))
                .sorted((v1, v2) -> v1.getValuationDate().compareTo(v2.getValuationDate()))
                .toList();

        if (recentValuations.size() < 2) {
            return BigDecimal.ZERO;
        }

        // Simple volatility calculation
        BigDecimal firstValue = recentValuations.get(0).getValue();
        BigDecimal lastValue = recentValuations.get(recentValuations.size() - 1).getValue();

        if (firstValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal change = lastValue.subtract(firstValue)
                .divide(firstValue, 4, RoundingMode.HALF_UP)
                .abs();

        return change;
    }

    /**
     * Get Valuation Statistics
     */
    public ValuationStatistics getValuationStatistics(Long assetId) {
        List<Valuation> valuations = valuationRepository.findByCollateralAssetId(assetId);

        if (valuations.isEmpty()) {
            return new ValuationStatistics(0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        long count = valuations.size();
        BigDecimal min = valuations.stream()
                .map(Valuation::getValue)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal max = valuations.stream()
                .map(Valuation::getValue)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal average = valuations.stream()
                .map(Valuation::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);

        return new ValuationStatistics(count, min, max, average);
    }

    // Private helper methods

    private void updatePortfolioTotalValue(Portfolio portfolio) {
        List<CollateralAsset> assets = collateralAssetRepository.findByPortfolioId(portfolio.getId());
        BigDecimal totalValue = assets.stream()
                .map(CollateralAsset::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        portfolio.setTotalValue(totalValue);
        portfolio.setLastModifiedAt(LocalDateTime.now());
        portfolioRepository.save(portfolio);
    }

    private void updateAllPortfolioValues() {
        List<Portfolio> portfolios = portfolioRepository.findAll();
        for (Portfolio portfolio : portfolios) {
            updatePortfolioTotalValue(portfolio);
        }
    }

    // Inner class for valuation statistics
    public static class ValuationStatistics {
        public final Long count;
        public final BigDecimal minValue;
        public final BigDecimal maxValue;
        public final BigDecimal averageValue;

        public ValuationStatistics(Long count, BigDecimal minValue, BigDecimal maxValue, BigDecimal averageValue) {
            this.count = count;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.averageValue = averageValue;
        }
    }
}