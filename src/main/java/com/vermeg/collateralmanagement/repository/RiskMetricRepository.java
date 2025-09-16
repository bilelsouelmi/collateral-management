package com.vermeg.collateralmanagement.repository;

import com.vermeg.collateralmanagement.entity.RiskMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RiskMetricRepository extends JpaRepository<RiskMetric, Long> {

    /**
     * Find risk metrics by portfolio ID
     */
    List<RiskMetric> findByPortfolioIdOrderByCalculationDateDesc(Long portfolioId);

    /**
     * Find latest risk metric for portfolio
     */
    @Query("SELECT rm FROM RiskMetric rm WHERE rm.portfolio.id = :portfolioId ORDER BY rm.calculationDate DESC LIMIT 1")
    Optional<RiskMetric> findLatestByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Find risk metrics by portfolio and date range
     */
    @Query("SELECT rm FROM RiskMetric rm WHERE rm.portfolio.id = :portfolioId AND rm.calculationDate BETWEEN :startDate AND :endDate ORDER BY rm.calculationDate DESC")
    List<RiskMetric> findByPortfolioIdAndCalculationDateBetween(@Param("portfolioId") Long portfolioId,
                                                                @Param("startDate") LocalDateTime startDate,
                                                                @Param("endDate") LocalDateTime endDate);

    /**
     * Find latest risk metrics by user ID
     */
    @Query("SELECT rm FROM RiskMetric rm WHERE rm.portfolio.user.id = :userId AND rm.id IN " +
            "(SELECT MAX(rm2.id) FROM RiskMetric rm2 WHERE rm2.portfolio.user.id = :userId GROUP BY rm2.portfolio.id)")
    List<RiskMetric> findLatestByUserId(@Param("userId") Long userId);

    /**
     * Find risk metrics by methodology
     */
    List<RiskMetric> findByMethodologyOrderByCalculationDateDesc(String methodology);

    /**
     * Find risk metrics with value greater than threshold
     */
    @Query("SELECT rm FROM RiskMetric rm WHERE rm.value > :threshold ORDER BY rm.value DESC")
    List<RiskMetric> findByValueGreaterThan(@Param("threshold") BigDecimal threshold);

    /**
     * Find risk metrics by value range
     */
    @Query("SELECT rm FROM RiskMetric rm WHERE rm.value BETWEEN :minValue AND :maxValue ORDER BY rm.calculationDate DESC")
    List<RiskMetric> findByValueBetween(@Param("minValue") BigDecimal minValue, @Param("maxValue") BigDecimal maxValue);

    /**
     * Find stale risk metrics (older than specified hours)
     */
    @Query("SELECT rm FROM RiskMetric rm WHERE rm.calculationDate < :cutoffDate")
    List<RiskMetric> findStaleRiskMetrics(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find high-risk portfolios
     */
    @Query("SELECT rm FROM RiskMetric rm WHERE rm.value > :riskThreshold AND rm.id IN " +
            "(SELECT MAX(rm2.id) FROM RiskMetric rm2 GROUP BY rm2.portfolio.id)")
    List<RiskMetric> findHighRiskPortfolios(@Param("riskThreshold") BigDecimal riskThreshold);

    /**
     * Get risk trend for portfolio (last N metrics)
     */
    @Query("SELECT rm FROM RiskMetric rm WHERE rm.portfolio.id = :portfolioId ORDER BY rm.calculationDate DESC LIMIT :limit")
    List<RiskMetric> findRiskTrendByPortfolio(@Param("portfolioId") Long portfolioId, @Param("limit") int limit);

    /**
     * Count risk metrics by portfolio
     */
    long countByPortfolioId(Long portfolioId);

    /**
     * Get average risk value by portfolio
     */
    @Query("SELECT AVG(rm.value) FROM RiskMetric rm WHERE rm.portfolio.id = :portfolioId")
    Optional<BigDecimal> getAverageRiskByPortfolio(@Param("portfolioId") Long portfolioId);

    /**
     * Get risk statistics by portfolio
     */
    @Query("SELECT rm.portfolio.id, COUNT(rm), MIN(rm.value), MAX(rm.value), AVG(rm.value) " +
            "FROM RiskMetric rm WHERE rm.portfolio.id = :portfolioId GROUP BY rm.portfolio.id")
    List<Object[]> getRiskStatisticsByPortfolio(@Param("portfolioId") Long portfolioId);

    /**
     * Find risk metrics created in date range
     */
    @Query("SELECT rm FROM RiskMetric rm WHERE rm.createdAt BETWEEN :startDate AND :endDate ORDER BY rm.createdAt DESC")
    List<RiskMetric> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Find portfolios needing risk recalculation
     */
    @Query("SELECT DISTINCT rm.portfolio.id FROM RiskMetric rm WHERE rm.calculationDate < :cutoffDate " +
            "AND rm.id IN (SELECT MAX(rm2.id) FROM RiskMetric rm2 GROUP BY rm2.portfolio.id)")
    List<Long> findPortfoliosNeedingRiskRecalculation(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete old risk metrics (keep only latest N per portfolio)
     */
    @Query("DELETE FROM RiskMetric rm WHERE rm.portfolio.id = :portfolioId AND rm.id NOT IN " +
            "(SELECT rm2.id FROM RiskMetric rm2 WHERE rm2.portfolio.id = :portfolioId ORDER BY rm2.calculationDate DESC LIMIT :keepCount)")
    void deleteOldRiskMetrics(@Param("portfolioId") Long portfolioId, @Param("keepCount") int keepCount);

    /**
     * Find risk metrics by user and date range
     */
    @Query("SELECT rm FROM RiskMetric rm WHERE rm.portfolio.user.id = :userId AND rm.calculationDate BETWEEN :startDate AND :endDate ORDER BY rm.calculationDate DESC")
    List<RiskMetric> findByUserIdAndCalculationDateBetween(@Param("userId") Long userId,
                                                           @Param("startDate") LocalDateTime startDate,
                                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Get latest risk metrics for all portfolios
     */
    @Query("SELECT rm FROM RiskMetric rm WHERE rm.id IN " +
            "(SELECT MAX(rm2.id) FROM RiskMetric rm2 GROUP BY rm2.portfolio.id) " +
            "ORDER BY rm.value DESC")
    List<RiskMetric> findLatestRiskMetricsForAllPortfolios();

    /**
     * Get risk metrics summary by methodology
     */
    @Query("SELECT rm.methodology, COUNT(rm), AVG(rm.value), MIN(rm.value), MAX(rm.value) " +
            "FROM RiskMetric rm GROUP BY rm.methodology")
    List<Object[]> getRiskMetricsSummaryByMethodology();

    /**
     * Find risk metrics with significant changes
     */
    @Query("SELECT rm1 FROM RiskMetric rm1 " +
            "JOIN RiskMetric rm2 ON rm1.portfolio.id = rm2.portfolio.id " +
            "WHERE rm1.calculationDate > rm2.calculationDate " +
            "AND ABS(rm1.value - rm2.value) / rm2.value > :threshold " +
            "ORDER BY rm1.calculationDate DESC")
    List<RiskMetric> findRiskMetricsWithSignificantChanges(@Param("threshold") BigDecimal threshold);

    /**
     * Check if portfolio has recent risk calculation
     */
    @Query("SELECT COUNT(rm) > 0 FROM RiskMetric rm WHERE rm.portfolio.id = :portfolioId AND rm.calculationDate > :recentDate")
    boolean hasRecentRiskCalculation(@Param("portfolioId") Long portfolioId, @Param("recentDate") LocalDateTime recentDate);

    /**
     * Find risk metrics by user in date range
     */
    @Query("SELECT rm FROM RiskMetric rm WHERE rm.portfolio.user.id = :userId AND rm.calculationDate BETWEEN :startDate AND :endDate ORDER BY rm.calculationDate DESC")
    List<RiskMetric> findByUserId(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find recent risk metrics for system analytics
     */
    @Query("SELECT rm FROM RiskMetric rm WHERE rm.calculationDate >= :since ORDER BY rm.calculationDate DESC")
    List<RiskMetric> findRecentRiskMetrics(@Param("since") LocalDateTime since);

    /**
     * Get risk trend data for user - using native query for DATE grouping
     */
    @Query(value = "SELECT DATE(calculation_date) as date, AVG(value) as avgRisk, MAX(value) as maxRisk, COUNT(*) as count " +
            "FROM risk_metrics rm JOIN portfolios p ON rm.portfolio_id = p.id " +
            "WHERE p.user_id = :userId AND rm.calculation_date >= :startDate " +
            "GROUP BY DATE(calculation_date) ORDER BY date DESC",
            nativeQuery = true)
    List<Object[]> getRiskTrendsByUser(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    /**
     * Get current risk summary by user
     */
    @Query("SELECT " +
            "COUNT(DISTINCT rm.portfolio.id) as portfolios, " +
            "AVG(rm.value) as avgRisk, " +
            "MAX(rm.value) as maxRisk, " +
            "MIN(rm.value) as minRisk " +
            "FROM RiskMetric rm WHERE rm.portfolio.user.id = :userId " +
            "AND rm.id IN (SELECT MAX(rm2.id) FROM RiskMetric rm2 WHERE rm2.portfolio.user.id = :userId GROUP BY rm2.portfolio.id)")
    Object[] getCurrentRiskSummaryByUser(@Param("userId") Long userId);

    /**
     * Find high risk portfolios by user
     */
    @Query("SELECT rm FROM RiskMetric rm WHERE rm.portfolio.user.id = :userId AND rm.value > :threshold " +
            "AND rm.id IN (SELECT MAX(rm2.id) FROM RiskMetric rm2 WHERE rm2.portfolio.user.id = :userId GROUP BY rm2.portfolio.id)")
    List<RiskMetric> findHighRiskPortfoliosByUser(@Param("userId") Long userId, @Param("threshold") BigDecimal threshold);

    /**
     * Get risk distribution by methodology
     */
    @Query("SELECT rm.methodology, COUNT(rm), AVG(rm.value) FROM RiskMetric rm WHERE rm.portfolio.user.id = :userId GROUP BY rm.methodology")
    List<Object[]> getRiskDistributionByMethodology(@Param("userId") Long userId);

}