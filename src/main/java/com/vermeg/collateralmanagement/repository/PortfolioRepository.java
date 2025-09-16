package com.vermeg.collateralmanagement.repository;

import com.vermeg.collateralmanagement.entity.Portfolio;
import com.vermeg.collateralmanagement.enums.PortfolioType;
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
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    /**
     * Find portfolios by user ID
     */
    List<Portfolio> findByUserId(Long userId);

    /**
     * Find portfolio by ID and user ID (for security)
     */
    Optional<Portfolio> findByIdAndUserId(Long id, Long userId);

    /**
     * Find portfolios by type
     */
    List<Portfolio> findByType(PortfolioType type);

    /**
     * Find portfolios by user ID and type
     */
    List<Portfolio> findByUserIdAndType(Long userId, PortfolioType type);

    /**
     * Check if portfolio exists by name and user
     */
    boolean existsByNameAndUserId(String name, Long userId);

    /**
     * Get portfolio count by user
     */
    long countByUserId(Long userId);

    /**
     * Find user's portfolios ordered by total value descending
     */
    List<Portfolio> findByUserIdOrderByTotalValueDesc(Long userId);

    /**
     * Find portfolios by name containing (case insensitive)
     */
    List<Portfolio> findByNameContainingIgnoreCase(String name);

    /**
     * Get total value of all user's portfolios
     */
    @Query("SELECT COALESCE(SUM(p.totalValue), 0) FROM Portfolio p WHERE p.user.id = :userId")
    BigDecimal getTotalValueByUserId(@Param("userId") Long userId);

    /**
     * Find portfolios with total value greater than specified amount
     */
    @Query("SELECT p FROM Portfolio p WHERE p.totalValue > :minValue")
    List<Portfolio> findPortfoliosWithValueGreaterThan(@Param("minValue") BigDecimal minValue);

    /**
     * Count portfolios created after specific date
     */
    @Query("SELECT COUNT(p) FROM Portfolio p WHERE p.createdAt >= :date")
    Long countPortfoliosCreatedAfter(@Param("date") LocalDateTime date);

    /**
     * Get total portfolio value across all portfolios
     */
    @Query("SELECT COALESCE(SUM(p.totalValue), 0) FROM Portfolio p")
    BigDecimal getTotalPortfolioValue();

    /**
     * Get average portfolio value by user
     */
    @Query("SELECT p.user.id, AVG(p.totalValue) FROM Portfolio p GROUP BY p.user.id")
    List<Object[]> getAveragePortfolioValueByUser();

    /**
     * Find portfolios created in date range for analytics
     */
    @Query("SELECT p FROM Portfolio p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<Portfolio> findPortfoliosCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Get portfolio statistics for dashboard
     */
    @Query("SELECT COUNT(p), SUM(p.totalValue), AVG(p.totalValue), MAX(p.totalValue) FROM Portfolio p WHERE p.user.id = :userId")
    Object[] getPortfolioStatsByUser(@Param("userId") Long userId);

    /**
     * Get portfolio distribution by type
     */
    @Query("SELECT p.type, COUNT(p), SUM(p.totalValue) FROM Portfolio p GROUP BY p.type")
    List<Object[]> getPortfolioDistributionByType();

    /**
     * Get user's portfolio performance summary
     */
    @Query("SELECT COUNT(p), SUM(p.totalValue), SUM(p.totalMargin) FROM Portfolio p WHERE p.user.id = :userId")
    Object[] getPortfolioPerformanceSummary(@Param("userId") Long userId);
}