package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.entity.CollateralAsset;
import com.vermeg.collateralmanagement.entity.Portfolio;
import com.vermeg.collateralmanagement.entity.Valuation;
import com.vermeg.collateralmanagement.repository.CollateralAssetRepository;
import com.vermeg.collateralmanagement.repository.PortfolioRepository;
import com.vermeg.collateralmanagement.repository.ValuationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CollateralAssetService {

    private static final Logger log = LoggerFactory.getLogger(CollateralAssetService.class);

    @Autowired
    private CollateralAssetRepository collateralAssetRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private ValuationRepository valuationRepository;

    /**
     * Add Collateral Asset - Use Case Implementation
     */
    public CollateralAsset addCollateralAsset(CollateralAsset asset, Long userId) {
        log.info("Adding collateral asset: {} for user ID: {}", asset.getName(), userId);

        // Validate asset ID uniqueness
        if (collateralAssetRepository.existsByAssetId(asset.getAssetId())) {
            throw new RuntimeException("Asset with ID '" + asset.getAssetId() + "' already exists");
        }

        // Set initial values
        asset.setIsEligible(true);

        CollateralAsset savedAsset = collateralAssetRepository.save(asset);

        // Create initial valuation record
        createInitialValuation(savedAsset);

        log.info("Collateral asset created successfully with ID: {}", savedAsset.getId());
        return savedAsset;
    }

    /**
     * Remove Asset - Use Case Implementation
     */
    public void removeAsset(Long assetId, Long userId) {
        log.info("Removing asset ID: {} for user ID: {}", assetId, userId);

        CollateralAsset asset = findAssetById(assetId);

        // Check if asset is in a portfolio
        if (asset.getPortfolio() != null) {
            // Verify user owns the portfolio
            Portfolio portfolio = asset.getPortfolio();
            if (!portfolio.getUser().getId().equals(userId)) {
                throw new RuntimeException("Access denied: Asset belongs to another user's portfolio");
            }

            // Remove from portfolio first
            asset.setPortfolio(null);
            collateralAssetRepository.save(asset);
        }

        // Delete the asset and all related valuations
        collateralAssetRepository.delete(asset);
        log.info("Asset removed successfully: {}", assetId);
    }

    /**
     * Assign Asset to Portfolio - Use Case Implementation
     */
    public CollateralAsset assignAssetToPortfolio(Long assetId, Long portfolioId, Long userId) {
        log.info("Assigning asset {} to portfolio {} for user {}", assetId, portfolioId, userId);

        CollateralAsset asset = findAssetById(assetId);
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found or access denied"));

        // Check if asset is already in a portfolio
        if (asset.getPortfolio() != null) {
            throw new RuntimeException("Asset is already assigned to portfolio: " + asset.getPortfolio().getName());
        }

        asset.setPortfolio(portfolio);
        CollateralAsset updatedAsset = collateralAssetRepository.save(asset);

        log.info("Asset assigned to portfolio successfully");
        return updatedAsset;
    }

    /**
     * View Asset Details - Use Case Implementation
     */
    public CollateralAsset getAssetDetails(Long assetId, Long userId) {
        log.debug("Getting asset details for ID: {} by user: {}", assetId, userId);

        CollateralAsset asset = findAssetById(assetId);

        // Verify user has access to this asset
        if (asset.getPortfolio() != null && !asset.getPortfolio().getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied: Asset belongs to another user");
        }

        return asset;
    }

    /**
     * Update asset information
     */
    public CollateralAsset updateAsset(Long assetId, CollateralAsset assetUpdate, Long userId) {
        log.info("Updating asset ID: {} for user ID: {}", assetId, userId);

        CollateralAsset existingAsset = getAssetDetails(assetId, userId);

        // Update allowed fields
        existingAsset.setName(assetUpdate.getName());
        existingAsset.setType(assetUpdate.getType());
        existingAsset.setMarketValue(assetUpdate.getMarketValue());
        existingAsset.setCurrency(assetUpdate.getCurrency());
        existingAsset.setHaircut(assetUpdate.getHaircut());
        existingAsset.setDueDate(assetUpdate.getDueDate());
        existingAsset.setIsEligible(assetUpdate.getIsEligible());

        CollateralAsset updatedAsset = collateralAssetRepository.save(existingAsset);

        // Create valuation record for the updated market value
        if (assetUpdate.getMarketValue() != null) {
            createValuation(updatedAsset, assetUpdate.getMarketValue());
        }

        log.info("Asset updated successfully: {}", assetId);
        return updatedAsset;
    }

    /**
     * Get user's assets
     */
    public List<CollateralAsset> getUserAssets(Long userId) {
        log.debug("Getting assets for user ID: {}", userId);
        return collateralAssetRepository.findByUserId(userId);
    }

    /**
     * Get unassigned assets
     */
    public List<CollateralAsset> getUnassignedAssets() {
        log.debug("Getting unassigned assets");
        return collateralAssetRepository.findUnassignedAssets();
    }

    /**
     * Get assets by portfolio
     */
    public List<CollateralAsset> getAssetsByPortfolio(Long portfolioId, Long userId) {
        // Verify user owns the portfolio
        portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found or access denied"));

        return collateralAssetRepository.findByPortfolioId(portfolioId);
    }

    /**
     * Update asset market value and create valuation
     */
    public CollateralAsset updateAssetValue(Long assetId, BigDecimal newValue, Long userId) {
        log.info("Updating asset value for ID: {} to {}", assetId, newValue);

        CollateralAsset asset = getAssetDetails(assetId, userId);
        asset.setMarketValue(newValue);

        CollateralAsset updatedAsset = collateralAssetRepository.save(asset);

        // Create valuation record
        createValuation(updatedAsset, newValue);

        return updatedAsset;
    }

    /**
     * Find asset by ID
     */
    public CollateralAsset findAssetById(Long assetId) {
        return collateralAssetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found with ID: " + assetId));
    }

    /**
     * Find asset by business asset ID
     */
    public CollateralAsset findAssetByAssetId(String assetId) {
        return collateralAssetRepository.findByAssetId(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found with asset ID: " + assetId));
    }

    /**
     * Create initial valuation record
     */
    private void createInitialValuation(CollateralAsset asset) {
        if (asset.getMarketValue() != null) {
            createValuation(asset, asset.getMarketValue());
        }
    }

    /**
     * Create valuation record
     */
    private void createValuation(CollateralAsset asset, BigDecimal value) {
        Valuation valuation = new Valuation();
        valuation.setCollateralAsset(asset);
        valuation.setValue(value);
        valuation.setCurrency(asset.getCurrency());
        valuation.setValuationDate(LocalDateTime.now());

        valuationRepository.save(valuation);
        log.debug("Created valuation record for asset: {}", asset.getAssetId());
    }

    /**
     * Get assets nearing maturity
     */
    public List<CollateralAsset> getAssetsNearingMaturity(int daysThreshold, Long userId) {
        LocalDateTime thresholdDate = LocalDateTime.now().plusDays(daysThreshold);
        List<CollateralAsset> nearingMaturity = collateralAssetRepository
                .findAssetsNearingMaturity(LocalDateTime.now(), thresholdDate);

        // Filter by user access
        return nearingMaturity.stream()
                .filter(asset -> asset.getPortfolio() != null &&
                        asset.getPortfolio().getUser().getId().equals(userId))
                .toList();
    }
}