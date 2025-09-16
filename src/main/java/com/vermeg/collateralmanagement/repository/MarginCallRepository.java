package com.vermeg.collateralmanagement.repository;

import com.vermeg.collateralmanagement.entity.MarginCall;
import com.vermeg.collateralmanagement.enums.MarginCallStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarginCallRepository extends JpaRepository<MarginCall, Long> {

    /**
     * Find active margin call by portfolio
     */
    @Query("SELECT mc FROM MarginCall mc WHERE mc.portfolio.id = :portfolioId AND mc.status != 'RESOLVED' ORDER BY mc.createdAt DESC")
    Optional<MarginCall> findActiveMarginCallByPortfolio(@Param("portfolioId") Long portfolioId);

    /**
     * Find active margin calls by user ID
     */
    @Query("SELECT mc FROM MarginCall mc WHERE mc.portfolio.user.id = :userId AND mc.status != 'RESOLVED' ORDER BY mc.createdAt DESC")
    List<MarginCall> findActiveMarginCallsByUserId(@Param("userId") Long userId);

    /**
     * Find margin calls by status
     */
    List<MarginCall> findByStatusOrderByCreatedAtDesc(MarginCallStatus status);

    /**
     * Find margin calls by portfolio ID
     */
    List<MarginCall> findByPortfolioIdOrderByCreatedAtDesc(Long portfolioId);

    /**
     * Find margin calls by portfolio ID and status
     */
    List<MarginCall> findByPortfolioIdAndStatusOrderByCreatedAtDesc(Long portfolioId, MarginCallStatus status);

    /**
     * Find overdue margin calls
     */
    @Query("SELECT mc FROM MarginCall mc WHERE mc.dueDate < :currentDate AND mc.status != 'RESOLVED'")
    List<MarginCall> findOverdueMarginCalls(@Param("currentDate") LocalDateTime currentDate);

    /**
     * Find margin calls due within specified days
     */
    @Query("SELECT mc FROM MarginCall mc WHERE mc.dueDate BETWEEN :startDate AND :endDate AND mc.status != 'RESOLVED'")
    List<MarginCall> findMarginCallsDueWithinPeriod(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Count margin calls by status
     */
    long countByStatus(MarginCallStatus status);

    /**
     * Count active margin calls by user
     */
    @Query("SELECT COUNT(mc) FROM MarginCall mc WHERE mc.portfolio.user.id = :userId AND mc.status != 'RESOLVED'")
    long countActiveMarginCallsByUserId(@Param("userId") Long userId);

    /**
     * Find margin calls with shortfall greater than specified amount
     */
    @Query("SELECT mc FROM MarginCall mc WHERE mc.shortfall > :minShortfall AND mc.status != 'RESOLVED'")
    List<MarginCall> findMarginCallsWithShortfallGreaterThan(@Param("minShortfall") BigDecimal minShortfall);

    /**
     * Find margin calls by user and status
     */
    @Query("SELECT mc FROM MarginCall mc WHERE mc.portfolio.user.id = :userId AND mc.status = :status ORDER BY mc.createdAt DESC")
    List<MarginCall> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") MarginCallStatus status);

    /**
     * Find margin calls created in date range
     */
    @Query("SELECT mc FROM MarginCall mc WHERE mc.createdAt BETWEEN :startDate AND :endDate ORDER BY mc.createdAt DESC")
    List<MarginCall> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Get margin call statistics by status
     */
    @Query("SELECT mc.status, COUNT(mc), SUM(mc.shortfall), AVG(mc.shortfall) FROM MarginCall mc GROUP BY mc.status")
    List<Object[]> getMarginCallStatistics();

    /**
     * Find urgent margin calls (due within 24 hours)
     */
    @Query("SELECT mc FROM MarginCall mc WHERE mc.dueDate <= :urgentDate AND mc.status != 'RESOLVED' ORDER BY mc.dueDate ASC")
    List<MarginCall> findUrgentMarginCalls(@Param("urgentDate") LocalDateTime urgentDate);

    /**
     * Find margin calls by portfolio and date range
     */
    @Query("SELECT mc FROM MarginCall mc WHERE mc.portfolio.id = :portfolioId AND mc.createdAt BETWEEN :startDate AND :endDate ORDER BY mc.createdAt DESC")
    List<MarginCall> findByPortfolioIdAndCreatedAtBetween(@Param("portfolioId") Long portfolioId,
                                                          @Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Check if portfolio has active margin calls
     */
    @Query("SELECT COUNT(mc) > 0 FROM MarginCall mc WHERE mc.portfolio.id = :portfolioId AND mc.status != 'RESOLVED'")
    boolean hasActiveMarginCalls(@Param("portfolioId") Long portfolioId);

    /**
     * Get latest margin call for portfolio
     */
    @Query("SELECT mc FROM MarginCall mc WHERE mc.portfolio.id = :portfolioId ORDER BY mc.createdAt DESC LIMIT 1")
    Optional<MarginCall> findLatestByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Find margin calls requiring immediate attention
     */
    @Query("SELECT mc FROM MarginCall mc WHERE " +
            "(mc.dueDate <= :urgentDate OR mc.status = 'OVERDUE') " +
            "AND mc.status != 'RESOLVED' " +
            "ORDER BY mc.dueDate ASC, mc.shortfall DESC")
    List<MarginCall> findMarginCallsRequiringAttention(@Param("urgentDate") LocalDateTime urgentDate);

    /**
     * Find resolved margin calls by date range
     */
    @Query("SELECT mc FROM MarginCall mc WHERE mc.status = 'RESOLVED' AND mc.lastModifiedAt BETWEEN :startDate AND :endDate")
    List<MarginCall> findResolvedMarginCallsByDateRange(@Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);
    /**
     * Find margin calls created after specific date
     */
    @Query("SELECT mc FROM MarginCall mc WHERE mc.createdAt >= :date ORDER BY mc.createdAt DESC")
    List<MarginCall> findMarginCallsCreatedAfter(@Param("date") LocalDateTime date);

    /**
     * Get margin call summary by user - using native query for complex aggregations
     */
    @Query(value = "SELECT " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN status != 'RESOLVED' THEN 1 ELSE 0 END) as active, " +
            "SUM(CASE WHEN due_date < NOW() AND status != 'RESOLVED' THEN 1 ELSE 0 END) as overdue, " +
            "SUM(shortfall) as totalShortfall, " +
            "AVG(shortfall) as avgShortfall " +
            "FROM margin_calls mc JOIN portfolios p ON mc.portfolio_id = p.id " +
            "WHERE p.user_id = :userId AND mc.created_at >= :since",
            nativeQuery = true)
    Object[] getMarginCallSummaryByUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Get margin call trends for charts
     */
    @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as count, SUM(shortfall) as totalShortfall " +
            "FROM margin_calls mc JOIN portfolios p ON mc.portfolio_id = p.id " +
            "WHERE p.user_id = :userId AND mc.created_at >= :startDate " +
            "GROUP BY DATE(created_at) ORDER BY date DESC",
            nativeQuery = true)
    List<Object[]> getMarginCallTrendsByUser(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    /**
     * Get margin call status distribution
     */
    @Query("SELECT mc.status, COUNT(mc), SUM(mc.shortfall) FROM MarginCall mc WHERE mc.portfolio.user.id = :userId GROUP BY mc.status")
    List<Object[]> getMarginCallStatusDistribution(@Param("userId") Long userId);

    /**
     * Find urgent margin calls (due within hours)
     */
    @Query("SELECT mc FROM MarginCall mc WHERE mc.portfolio.user.id = :userId AND mc.dueDate <= :urgentThreshold AND mc.status IN (:activeStatuses)")
    List<MarginCall> findUrgentMarginCallsByUser(@Param("userId") Long userId,
                                                 @Param("urgentThreshold") LocalDateTime urgentThreshold,
                                                 @Param("activeStatuses") List<MarginCallStatus> activeStatuses);

    // Helper method for above
    default List<MarginCall> findUrgentMarginCallsByUser(Long userId, int hoursThreshold) {
        LocalDateTime urgentThreshold = LocalDateTime.now().plusHours(hoursThreshold);
        List<MarginCallStatus> activeStatuses = List.of(MarginCallStatus.PENDING, MarginCallStatus.ACKNOWLEDGED, MarginCallStatus.OVERDUE);
        return findUrgentMarginCallsByUser(userId, urgentThreshold, activeStatuses);
    }
}