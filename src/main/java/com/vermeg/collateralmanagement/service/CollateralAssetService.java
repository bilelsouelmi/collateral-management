package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.dto.request.*;
import com.vermeg.collateralmanagement.dto.response.*;
import com.vermeg.collateralmanagement.dto.dashboard.AssetAllocationDto;
import com.vermeg.collateralmanagement.entity.CollateralAsset;
import com.vermeg.collateralmanagement.entity.Portfolio;
import com.vermeg.collateralmanagement.entity.Valuation;
import com.vermeg.collateralmanagement.enums.AssetStatus;
import com.vermeg.collateralmanagement.enums.AssetType;
import com.vermeg.collateralmanagement.mapper.AssetMapper;
import com.vermeg.collateralmanagement.repository.CollateralAssetRepository;
import com.vermeg.collateralmanagement.repository.PortfolioRepository;
import com.vermeg.collateralmanagement.repository.ValuationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private AssetMapper assetMapper;

    // ========== ASSET ID GENERATION ==========

    /**
     * Generate unique asset ID based on type
     */
    private String generateUniqueAssetId(AssetType type) {
        String prefix = getAssetTypePrefix(type);
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8); // Last 5 digits
        String randomSuffix = String.format("%03d", new Random().nextInt(1000));

        String assetId = prefix + "-" + timestamp + randomSuffix;

        // Ensure uniqueness
        int attempt = 0;
        while (collateralAssetRepository.existsByAssetId(assetId) && attempt < 10) {
            randomSuffix = String.format("%03d", new Random().nextInt(1000));
            assetId = prefix + "-" + timestamp + randomSuffix;
            attempt++;
        }

        return assetId;
    }

    /**
     * Get prefix for asset type
     */
    private String getAssetTypePrefix(AssetType type) {
        switch (type) {
            case CASH: return "CASH";
            case EQUITY: return "EQ";
            case GOVERNMENT_BOND: return "GB";
            case CORPORATE_BOND: return "CB";
            case COMMODITY: return "COMM";
            case REAL_ESTATE: return "RE";
            default: return "AST";
        }
    }

    // ========== NEW DTO-BASED METHODS ==========

    /**
     * Create Asset using DTO - NEW METHOD
     */
    public AssetResponse createAsset(CreateAssetRequest request, Long userId) {
        log.info("Creating asset: {} for user ID: {}", request.getName(), userId);

        // ✅ GÉNÉRATION AUTOMATIQUE de assetId si vide ou null
        if (request.getAssetId() == null || request.getAssetId().trim().isEmpty()) {
            String generatedId = generateUniqueAssetId(request.getType());
            request.setAssetId(generatedId);
            log.info("Generated asset ID: {}", generatedId);
        }

        // Validate asset ID uniqueness
        if (collateralAssetRepository.existsByAssetId(request.getAssetId())) {
            throw new RuntimeException("Asset with ID '" + request.getAssetId() + "' already exists");
        }

        // Convert DTO to entity
        CollateralAsset asset = assetMapper.toEntity(request);

        // Handle portfolio assignment if provided
        if (request.getPortfolioId() != null) {
            Portfolio portfolio = portfolioRepository.findByIdAndUserId(request.getPortfolioId(), userId)
                    .orElseThrow(() -> new RuntimeException("Portfolio not found or access denied"));
            asset.setPortfolio(portfolio);
        }

        // Save entity
        CollateralAsset savedAsset = collateralAssetRepository.save(asset);

        // Create initial valuation record
        createInitialValuation(savedAsset);

        log.info("Asset created successfully with ID: {} and assetId: {}",
                savedAsset.getId(), savedAsset.getAssetId());
        return assetMapper.toResponse(savedAsset);
    }

    /**
     * Update Asset using DTO - NEW METHOD
     */
    public AssetResponse updateAsset(Long id, UpdateAssetRequest request, Long userId) {
        log.info("Updating asset ID: {} for user ID: {}", id, userId);

        CollateralAsset existingAsset = findAssetById(id);

        // Verify user has access to this asset
        if (existingAsset.getPortfolio() != null &&
                !existingAsset.getPortfolio().getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied: Asset belongs to another user");
        }

        // Update entity from DTO
        assetMapper.updateEntity(existingAsset, request);

        // Handle portfolio reassignment if provided
        if (request.getPortfolioId() != null) {
            Portfolio portfolio = portfolioRepository.findByIdAndUserId(request.getPortfolioId(), userId)
                    .orElseThrow(() -> new RuntimeException("Portfolio not found or access denied"));
            existingAsset.setPortfolio(portfolio);
        }

        CollateralAsset updatedAsset = collateralAssetRepository.save(existingAsset);

        // Create valuation record if market value was updated
        if (request.getMarketValue() != null) {
            createValuation(updatedAsset, request.getMarketValue());
        }

        log.info("Asset updated successfully: {}", id);
        return assetMapper.toResponse(updatedAsset);
    }

    /**
     * Search Assets using DTO - NEW METHOD
     */
    public Page<AssetResponse> searchAssets(AssetSearchRequest request, Pageable pageable, Long userId) {
        log.debug("Searching assets for user ID: {} with filters", userId);

        // Use the repository method with all filters
        Page<CollateralAsset> assets = collateralAssetRepository.findAssetsWithFilters(
                request.getSearch(),
                request.getAssetId(),
                request.getName(),
                request.getType(),
                request.getStatus(),
                request.getCurrency(),
                request.getMinValue(),
                request.getMaxValue(),
                request.getDateFrom(),
                request.getDateTo(),
                request.getPortfolioId(),
                request.getIsEligible(),
                pageable
        );

        // Filter by user access and convert to DTOs
        return assets.map(asset -> {
            // Only return assets the user has access to
            if (asset.getPortfolio() != null &&
                    !asset.getPortfolio().getUser().getId().equals(userId)) {
                return null;
            }
            return assetMapper.toResponse(asset);
        }).map(response -> response != null ? response : null);
    }

    /**
     * Get Asset by ID returning DTO - NEW METHOD
     */
    public AssetResponse getAssetById(Long id, Long userId) {
        log.debug("Getting asset details for ID: {} by user: {}", id, userId);

        CollateralAsset asset = getAssetDetails(id, userId); // Use existing method
        return assetMapper.toResponse(asset);
    }

    /**
     * Get Asset Summary - NEW METHOD
     */
    public AssetSummaryResponse getAssetSummary(Long userId) {
        log.debug("Getting asset summary for user ID: {}", userId);

        List<CollateralAsset> userAssets = getUserAssets(userId);

        if (userAssets.isEmpty()) {
            return AssetSummaryResponse.builder()
                    .totalAssets(0)
                    .totalValue(BigDecimal.ZERO)
                    .averageValue(BigDecimal.ZERO)
                    .typeDistribution(List.of())
                    .statusDistribution(Map.of())
                    .currencyDistribution(Map.of())
                    .topValuedAssets(List.of())
                    .recentlyAddedAssets(List.of())
                    .eligibleAssetsCount(0)
                    .eligibleAssetsValue(BigDecimal.ZERO)
                    .nearMaturityCount(0)
                    .nearMaturityValue(BigDecimal.ZERO)
                    .build();
        }

        // Calculate summary statistics
        BigDecimal totalValue = userAssets.stream()
                .map(CollateralAsset::getMarketValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageValue = totalValue.divide(
                BigDecimal.valueOf(userAssets.size()), 2, RoundingMode.HALF_UP);

        // Type distribution
        Map<AssetType, Long> typeCountMap = userAssets.stream()
                .collect(Collectors.groupingBy(
                        CollateralAsset::getType,
                        Collectors.counting()));

        Map<AssetType, BigDecimal> typeValueMap = userAssets.stream()
                .collect(Collectors.groupingBy(
                        CollateralAsset::getType,
                        Collectors.mapping(
                                CollateralAsset::getMarketValue,
                                Collectors.reducing(BigDecimal.ZERO,
                                        value -> value != null ? value : BigDecimal.ZERO,
                                        BigDecimal::add))));

        List<AssetAllocationDto> typeDistribution = typeCountMap.entrySet().stream()
                .map(entry -> AssetAllocationDto.builder()
                        .assetType(entry.getKey().name())
                        .displayName(entry.getKey().name())
                        .count(entry.getValue().intValue())
                        .value(typeValueMap.get(entry.getKey()))
                        .percentage(typeValueMap.get(entry.getKey())
                                .divide(totalValue, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .doubleValue())
                        .status("ACTIVE")
                        .build())
                .collect(Collectors.toList());

        // Status distribution
        Map<String, Integer> statusDistribution = userAssets.stream()
                .collect(Collectors.groupingBy(
                        asset -> asset.getStatus().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        // Currency distribution
        Map<String, BigDecimal> currencyDistribution = userAssets.stream()
                .filter(asset -> asset.getCurrency() != null)
                .collect(Collectors.groupingBy(
                        CollateralAsset::getCurrency,
                        Collectors.mapping(
                                CollateralAsset::getMarketValue,
                                Collectors.reducing(BigDecimal.ZERO,
                                        value -> value != null ? value : BigDecimal.ZERO,
                                        BigDecimal::add))));

        // Top valued assets (top 5)
        List<AssetResponse> topValuedAssets = userAssets.stream()
                .filter(asset -> asset.getMarketValue() != null)
                .sorted((a, b) -> b.getMarketValue().compareTo(a.getMarketValue()))
                .limit(5)
                .map(assetMapper::toResponse)
                .collect(Collectors.toList());

        // Recently added assets (last 5)
        List<AssetResponse> recentlyAddedAssets = userAssets.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .map(assetMapper::toResponse)
                .collect(Collectors.toList());

        // Eligible assets
        List<CollateralAsset> eligibleAssets = userAssets.stream()
                .filter(asset -> asset.getIsEligible() && asset.getStatus() == AssetStatus.ACTIVE)
                .collect(Collectors.toList());

        BigDecimal eligibleAssetsValue = eligibleAssets.stream()
                .map(CollateralAsset::getMarketValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Near maturity assets (within 30 days)
        List<CollateralAsset> nearMaturityAssets = userAssets.stream()
                .filter(asset -> asset.getDueDate() != null &&
                        asset.getDueDate().isBefore(LocalDateTime.now().plusDays(30)))
                .collect(Collectors.toList());

        BigDecimal nearMaturityValue = nearMaturityAssets.stream()
                .map(CollateralAsset::getMarketValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AssetSummaryResponse.builder()
                .totalAssets(userAssets.size())
                .totalValue(totalValue)
                .averageValue(averageValue)
                .typeDistribution(typeDistribution)
                .statusDistribution(statusDistribution)
                .currencyDistribution(currencyDistribution)
                .topValuedAssets(topValuedAssets)
                .recentlyAddedAssets(recentlyAddedAssets)
                .eligibleAssetsCount(eligibleAssets.size())
                .eligibleAssetsValue(eligibleAssetsValue)
                .nearMaturityCount(nearMaturityAssets.size())
                .nearMaturityValue(nearMaturityValue)
                .build();
    }

    /**
     * Bulk Create Assets - NEW METHOD
     */
    public BulkAssetResponse bulkCreateAssets(List<CreateAssetRequest> requests, Long userId) {
        log.info("Bulk creating {} assets for user ID: {}", requests.size(), userId);

        List<String> errors = new ArrayList<>();
        List<AssetResponse> processedAssets = new ArrayList<>();
        int successful = 0;
        int failed = 0;

        for (CreateAssetRequest request : requests) {
            try {
                // Generate asset ID if not provided
                if (request.getAssetId() == null || request.getAssetId().trim().isEmpty()) {
                    request.setAssetId(generateUniqueAssetId(request.getType()));
                }

                // Check if asset ID already exists
                if (collateralAssetRepository.existsByAssetId(request.getAssetId())) {
                    errors.add("Asset ID '" + request.getAssetId() + "' already exists");
                    failed++;
                    continue;
                }

                // Create asset
                AssetResponse createdAsset = createAsset(request, userId);
                processedAssets.add(createdAsset);
                successful++;

            } catch (Exception e) {
                errors.add("Failed to create asset '" + request.getAssetId() + "': " + e.getMessage());
                failed++;
            }
        }

        return BulkAssetResponse.builder()
                .totalProcessed(requests.size())
                .successful(successful)
                .failed(failed)
                .errors(errors)
                .processedAssets(processedAssets)
                .build();
    }

    // ========== EXISTING METHODS (UNCHANGED) ==========

    /**
     * Add Collateral Asset - Use Case Implementation
     */
    public CollateralAsset addCollateralAsset(CollateralAsset asset, Long userId) {
        log.info("Adding collateral asset: {} for user ID: {}", asset.getName(), userId);

        // ✅ GÉNÉRATION AUTOMATIQUE de assetId si vide ou null
        if (asset.getAssetId() == null || asset.getAssetId().trim().isEmpty()) {
            String generatedId = generateUniqueAssetId(asset.getType());
            asset.setAssetId(generatedId);
            log.info("Generated asset ID: {}", generatedId);
        }

        // Validate asset ID uniqueness
        if (collateralAssetRepository.existsByAssetId(asset.getAssetId())) {
            throw new RuntimeException("Asset with ID '" + asset.getAssetId() + "' already exists");
        }

        // Set initial values
        asset.setIsEligible(true);

        CollateralAsset savedAsset = collateralAssetRepository.save(asset);

        // Create initial valuation record
        createInitialValuation(savedAsset);

        log.info("Collateral asset created successfully with ID: {} and assetId: {}",
                savedAsset.getId(), savedAsset.getAssetId());
        return savedAsset;
    }

    /**
     * Remove Asset - Use Case Implementation with enhanced logging
     */
    public void removeAsset(Long assetId, Long userId) {
        log.info("Removing asset ID: {} for user ID: {}", assetId, userId);

        try {
            CollateralAsset asset = findAssetById(assetId);
            log.info("Found asset: {} with assetId: {}", asset.getId(), asset.getAssetId());

            // Check if asset is in a portfolio
            if (asset.getPortfolio() != null) {
                log.info("Asset has portfolio: {}", asset.getPortfolio().getId());
                // Verify user owns the portfolio
                Portfolio portfolio = asset.getPortfolio();
                if (!portfolio.getUser().getId().equals(userId)) {
                    throw new RuntimeException("Access denied: Asset belongs to another user's portfolio");
                }

                // Remove from portfolio first
                log.info("Removing asset from portfolio...");
                asset.setPortfolio(null);
                collateralAssetRepository.save(asset);
                log.info("Asset removed from portfolio");
            } else {
                log.info("Asset is unassigned (no portfolio)");
            }

            // Delete the asset and all related valuations (cascade should handle this)
            log.info("Attempting to delete asset...");
            collateralAssetRepository.delete(asset);
            log.info("Asset removed successfully: {}", assetId);

        } catch (Exception e) {
            log.error("Error removing asset {}: {}", assetId, e.getMessage(), e);
            throw new RuntimeException("Failed to remove asset: " + e.getMessage(), e);
        }
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

    // ========== PRIVATE HELPER METHODS ==========

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
}