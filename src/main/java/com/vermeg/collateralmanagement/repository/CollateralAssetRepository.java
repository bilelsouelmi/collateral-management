package com.vermeg.collateralmanagement.repository;

import com.vermeg.collateralmanagement.entity.CollateralAsset;
import com.vermeg.collateralmanagement.enums.AssetType;
import com.vermeg.collateralmanagement.enums.AssetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollateralAssetRepository extends JpaRepository<CollateralAsset, Long>, JpaSpecificationExecutor<CollateralAsset> {

    // ========== BASIC FINDER METHODS ==========

    /**
     * Find assets by portfolio ID
     */
    List<CollateralAsset> findByPortfolioId(Long portfolioId);

    /**
     * Find asset by asset ID (business identifier)
     */
    Optional<CollateralAsset> findByAssetId(String assetId);

    /**
     * Check if asset exists by asset ID
     */
    boolean existsByAssetId(String assetId);

    /**
     * Find assets by type
     */
    List<CollateralAsset> findByType(AssetType type);

    /**
     * Find assets by status
     */
    List<CollateralAsset> findByStatus(AssetStatus status);

    /**
     * Find assets by portfolio ID and status
     */
    List<CollateralAsset> findByPortfolioIdAndStatus(Long portfolioId, AssetStatus status);

    /**
     * Find eligible assets
     */
    List<CollateralAsset> findByIsEligibleTrue();

    /**
     * Find assets by portfolio and eligible status
     */
    List<CollateralAsset> findByPortfolioIdAndIsEligibleTrue(Long portfolioId);

    /**
     * Find assets by currency
     */
    List<CollateralAsset> findByCurrency(String currency);

    /**
     * Find assets by name containing (case insensitive)
     */
    List<CollateralAsset> findByNameContainingIgnoreCase(String name);

    /**
     * Find assets by portfolio and type
     */
    List<CollateralAsset> findByPortfolioIdAndType(Long portfolioId, AssetType type);

    // ========== CUSTOM QUERY METHODS ==========

    /**
     * Find unassigned assets (not in any portfolio)
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.portfolio IS NULL")
    List<CollateralAsset> findUnassignedAssets();

    /**
     * Find assets by user ID (through portfolio relationship) OR unassigned assets
     * FIXED: Proper handling of null portfolios
     */
    @Query("SELECT a FROM CollateralAsset a LEFT JOIN a.portfolio p WHERE p IS NULL OR p.user.id = :userId")
    List<CollateralAsset> findByUserId(@Param("userId") Long userId);
    /**
     * Find assets by user ID and status
     */
    @Query("SELECT a FROM CollateralAsset a WHERE (a.portfolio IS NULL OR a.portfolio.user.id = :userId) AND a.status = :status")
    List<CollateralAsset> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") AssetStatus status);

    /**
     * Find assets nearing maturity
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.dueDate IS NOT NULL AND a.dueDate BETWEEN :startDate AND :endDate")
    List<CollateralAsset> findAssetsNearingMaturity(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Find assets with market value greater than specified amount
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.marketValue > :minValue")
    List<CollateralAsset> findAssetsWithValueGreaterThan(@Param("minValue") BigDecimal minValue);

    /**
     * Find assets created in date range
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<CollateralAsset> findAssetsCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Find assets with stale valuations (older than threshold)
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.lastModifiedAt < :threshold")
    List<CollateralAsset> findAssetsWithStaleValuations(@Param("threshold") LocalDateTime threshold);

    /**
     * Find eligible assets for pledging by user
     */
    @Query("SELECT a FROM CollateralAsset a WHERE (a.portfolio IS NULL OR a.portfolio.user.id = :userId) AND a.status = :status AND a.isEligible = true")
    List<CollateralAsset> findEligibleAssetsForPledging(@Param("userId") Long userId, @Param("status") AssetStatus status);

    /**
     * Bulk find assets by asset IDs
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.assetId IN :assetIds")
    List<CollateralAsset> findByAssetIdIn(@Param("assetIds") List<String> assetIds);

    // ========== NEW DTO SEARCH METHOD ==========

    /**
     * Advanced search with filters - REQUIRED FOR DTO FUNCTIONALITY
     */
    @Query("SELECT a FROM CollateralAsset a WHERE " +
            "(:search IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(a.assetId) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:assetId IS NULL OR a.assetId = :assetId) AND " +
            "(:name IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:type IS NULL OR a.type = :type) AND " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:currency IS NULL OR a.currency = :currency) AND " +
            "(:minValue IS NULL OR a.marketValue >= :minValue) AND " +
            "(:maxValue IS NULL OR a.marketValue <= :maxValue) AND " +
            "(:dateFrom IS NULL OR a.createdAt >= :dateFrom) AND " +
            "(:dateTo IS NULL OR a.createdAt <= :dateTo) AND " +
            "(:portfolioId IS NULL OR a.portfolio.id = :portfolioId) AND " +
            "(:isEligible IS NULL OR a.isEligible = :isEligible)")
    Page<CollateralAsset> findAssetsWithFilters(
            @Param("search") String search,
            @Param("assetId") String assetId,
            @Param("name") String name,
            @Param("type") AssetType type,
            @Param("status") AssetStatus status,
            @Param("currency") String currency,
            @Param("minValue") BigDecimal minValue,
            @Param("maxValue") BigDecimal maxValue,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("portfolioId") Long portfolioId,
            @Param("isEligible") Boolean isEligible,
            Pageable pageable
    );

    // ========== AGGREGATION & STATISTICS METHODS ==========

    /**
     * Get total market value by portfolio
     */
    @Query("SELECT COALESCE(SUM(a.marketValue), 0) FROM CollateralAsset a WHERE a.portfolio.id = :portfolioId")
    BigDecimal getTotalMarketValueByPortfolio(@Param("portfolioId") Long portfolioId);

    /**
     * Get asset count by portfolio
     */
    long countByPortfolioId(Long portfolioId);

    /**
     * Get asset count by type
     */
    long countByType(AssetType type);

    /**
     * Count assets created after specific date
     */
    @Query("SELECT COUNT(a) FROM CollateralAsset a WHERE a.createdAt >= :date")
    Long countAssetsCreatedAfter(@Param("date") LocalDateTime date);

    /**
     * Get total asset value across all assets
     */
    @Query("SELECT COALESCE(SUM(a.marketValue), 0) FROM CollateralAsset a")
    BigDecimal getTotalAssetValue();

    /**
     * Get total asset value by user (includes unassigned assets)
     */
    @Query("SELECT COALESCE(SUM(a.marketValue), 0) FROM CollateralAsset a WHERE a.portfolio IS NULL OR a.portfolio.user.id = :userId")
    BigDecimal getTotalAssetValueByUser(@Param("userId") Long userId);

    /**
     * Count active assets
     */
    @Query("SELECT COUNT(a) FROM CollateralAsset a WHERE a.status = 'ACTIVE'")
    Long countActiveAssets();

    /**
     * Get total active asset value
     */
    @Query("SELECT COALESCE(SUM(a.marketValue), 0) FROM CollateralAsset a WHERE a.status = 'ACTIVE'")
    BigDecimal getTotalActiveValue();

    /**
     * Count eligible assets
     */
    @Query("SELECT COUNT(a) FROM CollateralAsset a WHERE a.isEligible = true AND a.status = 'ACTIVE'")
    Long countEligibleAssets();

    /**
     * Get total eligible asset value
     */
    @Query("SELECT COALESCE(SUM(a.marketValue), 0) FROM CollateralAsset a WHERE a.isEligible = true AND a.status = 'ACTIVE'")
    BigDecimal getTotalEligibleValue();

    // ========== DISTRIBUTION & BREAKDOWN METHODS ==========

    /**
     * Get asset allocation by type for user (includes unassigned assets)
     */
    @Query("SELECT a.type, COUNT(a), SUM(a.marketValue) FROM CollateralAsset a WHERE a.portfolio IS NULL OR a.portfolio.user.id = :userId GROUP BY a.type")
    List<Object[]> getAssetAllocationByUser(@Param("userId") Long userId);

    /**
     * Get asset allocation by type for all users
     */
    @Query("SELECT a.type, COUNT(a), SUM(a.marketValue) FROM CollateralAsset a GROUP BY a.type")
    List<Object[]> getGlobalAssetAllocation();

    /**
     * Get asset type distribution
     */
    @Query("SELECT a.type, COUNT(a), SUM(a.marketValue) FROM CollateralAsset a GROUP BY a.type")
    List<Object[]> getAssetTypeDistribution();

    /**
     * Get asset status distribution
     */
    @Query("SELECT a.status, COUNT(a) FROM CollateralAsset a GROUP BY a.status")
    List<Object[]> getAssetStatusDistribution();

    /**
     * Get asset currency distribution
     */
    @Query("SELECT a.currency, SUM(a.marketValue) FROM CollateralAsset a GROUP BY a.currency")
    List<Object[]> getAssetCurrencyDistribution();

    /**
     * Get asset status distribution for user (includes unassigned assets)
     */
    @Query("SELECT a.status, COUNT(a), SUM(a.marketValue) FROM CollateralAsset a WHERE a.portfolio IS NULL OR a.portfolio.user.id = :userId GROUP BY a.status")
    List<Object[]> getAssetStatusDistributionByUser(@Param("userId") Long userId);

    // ========== TOP ASSETS & RECENT ACTIVITY ==========

    /**
     * Find top valued assets
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.status = 'ACTIVE' ORDER BY a.marketValue DESC")
    List<CollateralAsset> findTopValuedAssets(Pageable pageable);

    /**
     * Find recently added assets
     */
    @Query("SELECT a FROM CollateralAsset a ORDER BY a.createdAt DESC")
    List<CollateralAsset> findRecentlyAddedAssets(Pageable pageable);

    /**
     * Find top valued assets by user (includes unassigned assets)
     */
    @Query("SELECT a FROM CollateralAsset a WHERE (a.portfolio IS NULL OR a.portfolio.user.id = :userId) AND a.status = 'ACTIVE' ORDER BY a.marketValue DESC")
    List<CollateralAsset> findTopValuedAssetsByUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find recently added assets by user (includes unassigned assets)
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.portfolio IS NULL OR a.portfolio.user.id = :userId ORDER BY a.createdAt DESC")
    List<CollateralAsset> findRecentlyAddedAssetsByUser(@Param("userId") Long userId, Pageable pageable);

    // ========== MATURITY & RISK ANALYSIS ==========

    /**
     * Find assets near maturity (within specified date)
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.dueDate IS NOT NULL AND a.dueDate <= :date")
    List<CollateralAsset> findAssetsNearMaturity(@Param("date") LocalDateTime date);

    /**
     * Find assets near maturity by user (includes unassigned assets)
     */
    @Query("SELECT a FROM CollateralAsset a WHERE (a.portfolio IS NULL OR a.portfolio.user.id = :userId) AND a.dueDate IS NOT NULL AND a.dueDate <= :date")
    List<CollateralAsset> findAssetsNearMaturityByUser(@Param("userId") Long userId, @Param("date") LocalDateTime date);

    /**
     * Count assets near maturity
     */
    @Query("SELECT COUNT(a) FROM CollateralAsset a WHERE a.dueDate IS NOT NULL AND a.dueDate <= :date")
    Long countAssetsNearMaturity(@Param("date") LocalDateTime date);

    /**
     * Get total value of assets near maturity
     */
    @Query("SELECT COALESCE(SUM(a.marketValue), 0) FROM CollateralAsset a WHERE a.dueDate IS NOT NULL AND a.dueDate <= :date")
    BigDecimal getTotalValueAssetsNearMaturity(@Param("date") LocalDateTime date);

    // ========== HELPER METHODS ==========

    /**
     * Helper method for eligible assets for pledging with default status
     */
    default List<CollateralAsset> findEligibleAssetsForPledging(Long userId) {
        return findEligibleAssetsForPledging(userId, AssetStatus.ACTIVE);
    }

    /**
     * Find assets by multiple criteria (alternative to filters) - includes unassigned assets
     */
    @Query("SELECT a FROM CollateralAsset a WHERE " +
            "(:userId IS NULL OR a.portfolio IS NULL OR a.portfolio.user.id = :userId) AND " +
            "(:type IS NULL OR a.type = :type) AND " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:isEligible IS NULL OR a.isEligible = :isEligible)")
    List<CollateralAsset> findByCriteria(@Param("userId") Long userId,
                                         @Param("type") AssetType type,
                                         @Param("status") AssetStatus status,
                                         @Param("isEligible") Boolean isEligible);

    /**
     * Find assets with value in range
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.marketValue BETWEEN :minValue AND :maxValue")
    List<CollateralAsset> findAssetsWithValueInRange(@Param("minValue") BigDecimal minValue,
                                                     @Param("maxValue") BigDecimal maxValue);

    /**
     * Find assets modified recently
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.lastModifiedAt >= :since ORDER BY a.lastModifiedAt DESC")
    List<CollateralAsset> findRecentlyModified(@Param("since") LocalDateTime since);
}