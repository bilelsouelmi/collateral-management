package com.vermeg.collateralmanagement.repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.vermeg.collateralmanagement.entity.Valuation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDateTime;
@Repository
public interface ValuationRepository extends JpaRepository<Valuation, Long> {

    /**
     * Find valuations by collateral asset ID
     */
    List<Valuation> findByCollateralAssetId(Long assetId);

    /**
     * Find valuations by currency
     */
    List<Valuation> findByCurrency(String currency);

    /**
     * Get valuation count by asset
     */
    long countByCollateralAssetId(Long assetId);

    /**
     * Find valuations in date range
     */
    @Query("SELECT v FROM Valuation v WHERE v.valuationDate BETWEEN :startDate AND :endDate ORDER BY v.valuationDate DESC")
    List<Valuation> findValuationsInDateRange(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * Find latest valuation for each asset by user
     */
    @Query("SELECT v FROM Valuation v WHERE v.collateralAsset.portfolio.user.id = :userId " +
            "AND v.id IN (SELECT MAX(v2.id) FROM Valuation v2 WHERE v2.collateralAsset.portfolio.user.id = :userId GROUP BY v2.collateralAsset.id)")
    List<Valuation> findLatestValuationsByUser(@Param("userId") Long userId);

    /**
     * Get valuation statistics by user
     */
    @Query("SELECT COUNT(v), SUM(v.value), AVG(v.value), MAX(v.value), MIN(v.value) FROM Valuation v WHERE v.collateralAsset.portfolio.user.id = :userId AND v.valuationDate >= :since")
    Object[] getValuationStatsByUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Find stale valuations (older than threshold)
     */
    @Query("SELECT v FROM Valuation v WHERE v.valuationDate < :threshold")
    List<Valuation> findStaleValuations(@Param("threshold") LocalDateTime threshold);

    /**
     * Get valuation trends by user
     */
    @Query(value = "SELECT DATE(valuation_date) as date, COUNT(*) as count, SUM(value) as totalValue, AVG(value) as avgValue " +
            "FROM valuations v JOIN collateral_assets ca ON v.collateral_asset_id = ca.id " +
            "JOIN portfolios p ON ca.portfolio_id = p.id " +
            "WHERE p.user_id = :userId AND v.valuation_date >= :startDate " +
            "GROUP BY DATE(valuation_date) ORDER BY date DESC",
            nativeQuery = true)
    List<Object[]> getValuationTrendsByUser(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    /**
     * Find latest valuations by currency
     */
    @Query("SELECT v FROM Valuation v WHERE v.currency = :currency " +
            "AND v.id IN (SELECT MAX(v2.id) FROM Valuation v2 WHERE v2.currency = :currency GROUP BY v2.collateralAsset.id)")
    List<Valuation> findLatestValuationsByCurrency(@Param("currency") String currency);
}