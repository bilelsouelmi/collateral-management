package com.vermeg.collateralmanagement.repository;

import com.vermeg.collateralmanagement.entity.CollateralAsset;
import com.vermeg.collateralmanagement.enums.AssetType;
import com.vermeg.collateralmanagement.enums.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollateralAssetRepository extends JpaRepository<CollateralAsset, Long> {

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
     * Find unassigned assets (not in any portfolio)
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.portfolio IS NULL")
    List<CollateralAsset> findUnassignedAssets();

    /**
     * Find assets by name containing (case insensitive)
     */
    List<CollateralAsset> findByNameContainingIgnoreCase(String name);

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
     * Find assets by portfolio and type
     */
    List<CollateralAsset> findByPortfolioIdAndType(Long portfolioId, AssetType type);

    /**
     * Find assets by user ID (through portfolio relationship)
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.portfolio.user.id = :userId")
    List<CollateralAsset> findByUserId(@Param("userId") Long userId);

    /**
     * Find assets by user ID and status
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.portfolio.user.id = :userId AND a.status = :status")
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
     * Get total asset value by user
     */
    @Query("SELECT COALESCE(SUM(a.marketValue), 0) FROM CollateralAsset a WHERE a.portfolio.user.id = :userId")
    BigDecimal getTotalAssetValueByUser(@Param("userId") Long userId);

    /**
     * Find assets created in date range
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<CollateralAsset> findAssetsCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Get asset allocation by type for user
     */
    @Query("SELECT a.type, COUNT(a), SUM(a.marketValue) FROM CollateralAsset a WHERE a.portfolio.user.id = :userId GROUP BY a.type")
    List<Object[]> getAssetAllocationByUser(@Param("userId") Long userId);

    /**
     * Get asset allocation by type for all users
     */
    @Query("SELECT a.type, COUNT(a), SUM(a.marketValue) FROM CollateralAsset a GROUP BY a.type")
    List<Object[]> getGlobalAssetAllocation();

    /**
     * Find assets with stale valuations (older than threshold)
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.lastModifiedAt < :threshold")
    List<CollateralAsset> findAssetsWithStaleValuations(@Param("threshold") LocalDateTime threshold);

    /**
     * Get asset status distribution for user
     */
    @Query("SELECT a.status, COUNT(a), SUM(a.marketValue) FROM CollateralAsset a WHERE a.portfolio.user.id = :userId GROUP BY a.status")
    List<Object[]> getAssetStatusDistributionByUser(@Param("userId") Long userId);

    /**
     * Find eligible assets for pledging by user
     */
    @Query("SELECT a FROM CollateralAsset a WHERE a.portfolio.user.id = :userId AND a.status = :status AND a.isEligible = true")
    List<CollateralAsset> findEligibleAssetsForPledging(@Param("userId") Long userId, @Param("status") AssetStatus status);

    // Helper method for above
    default List<CollateralAsset> findEligibleAssetsForPledging(Long userId) {
        return findEligibleAssetsForPledging(userId, AssetStatus.ACTIVE);
    }
}