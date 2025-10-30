// ReportRepository.java - Fixed with Database-Compatible Queries
package com.vermeg.collateralmanagement.repository;

import com.vermeg.collateralmanagement.entity.Portfolio;
import com.vermeg.collateralmanagement.entity.Report;
import com.vermeg.collateralmanagement.entity.User;
import com.vermeg.collateralmanagement.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    // ==================== BASIC QUERIES ====================
    /**
     * Find reports by user ordered by creation date (most recent first)
     */
    List<Report> findByUserOrderByCreatedAtDesc(User user);
    /**
     * Find reports by user with pagination
     */
    Page<Report> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    /**
     * Find reports by type ordered by creation date
     */
    List<Report> findByTypeOrderByCreatedAtDesc(ReportType type);
    /**
     * Find reports by type with pagination
     */
    Page<Report> findByTypeOrderByCreatedAtDesc(ReportType type, Pageable pageable);
    // ==================== STATUS-BASED QUERIES ====================
    /**
     * Find all pending reports (not yet generated)
     */
    @Query("SELECT r FROM Report r WHERE r.generatedAt IS NULL ORDER BY r.createdAt DESC")
    List<Report> findPendingReports();
    /**
     * Find all generated reports
     */
    @Query("SELECT r FROM Report r WHERE r.generatedAt IS NOT NULL ORDER BY r.generatedAt DESC")
    List<Report> findGeneratedReports();

    // ==================== DATE RANGE QUERIES ====================
    /**
     * Find reports created between dates
     */
    @Query("SELECT r FROM Report r WHERE r.createdAt BETWEEN :startDate AND :endDate ORDER BY r.createdAt DESC")
    List<Report> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);
    /**
     * Find reports generated between dates
     */
    @Query("SELECT r FROM Report r WHERE r.generatedAt BETWEEN :startDate AND :endDate ORDER BY r.generatedAt DESC")
    List<Report> findByGeneratedAtBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
    // ==================== USER AND TYPE COMBINATION QUERIES ====================
    /**
     * Find reports by user and type
     */
    @Query("SELECT r FROM Report r WHERE r.user = :user AND r.type = :type ORDER BY r.createdAt DESC")
    List<Report> findByUserAndType(@Param("user") User user, @Param("type") ReportType type);
    /**
     * Find reports by user ID (convenience method)
     */
    @Query("SELECT r FROM Report r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<Report> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    /**
     * Find portfolio by ID and user ID
     */
    @Query("SELECT p FROM Portfolio p WHERE p.id = :portfolioId AND p.user.id = :userId")
    Optional<Portfolio> findByIdAndUserId(@Param("portfolioId") Long portfolioId, @Param("userId") Long userId);

    // ==================== STATISTICS QUERIES ====================
    /**
     * Count reports created after a specific date
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.createdAt >= :date")
    Long countReportsCreatedAfter(@Param("date") LocalDateTime date);
    /**
     * Count reports generated after a specific date
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.generatedAt >= :date")
    Long countReportsGeneratedAfter(@Param("date") LocalDateTime date);
    /**
     * Count reports by type after a specific date
     */
    @Query("SELECT r.type, COUNT(r) FROM Report r WHERE r.createdAt >= :date GROUP BY r.type")
    List<Object[]> countReportsByTypeAfter(@Param("date") LocalDateTime date);
    /**
     * Count reports by user after a specific date
     */
    @Query("SELECT r.user.username, COUNT(r) FROM Report r WHERE r.createdAt >= :date GROUP BY r.user.username")
    List<Object[]> countReportsByUserAfter(@Param("date") LocalDateTime date);
    // ==================== RECENT REPORTS QUERIES ====================
    /**
     * Find recently generated reports (within threshold)
     */
    @Query("SELECT r FROM Report r WHERE r.generatedAt >= :threshold ORDER BY r.generatedAt DESC")
    List<Report> findRecentlyGeneratedReports(@Param("threshold") LocalDateTime threshold);
    /**
     * Find recently created reports (within threshold)
     */
    @Query("SELECT r FROM Report r WHERE r.createdAt >= :threshold ORDER BY r.createdAt DESC")
    List<Report> findRecentlyCreatedReports(@Param("threshold") LocalDateTime threshold);
    // ==================== LATEST REPORT QUERIES ====================
    /**
     * Find latest generated report by type for user
     */
    @Query("SELECT r FROM Report r WHERE r.user = :user AND r.type = :type AND r.generatedAt IS NOT NULL ORDER BY r.generatedAt DESC")
    List<Report> findLatestGeneratedReportByUserAndTypeList(@Param("user") User user, @Param("type") ReportType type);
    /**
     * Helper method to get the first result
     */
    default Optional<Report> findLatestGeneratedReportByUserAndType(User user, ReportType type) {
        List<Report> reports = findLatestGeneratedReportByUserAndTypeList(user, type);
        return reports.isEmpty() ? Optional.empty() : Optional.of(reports.get(0));
    }
    /**
     * Find latest report by user
     */
    @Query("SELECT r FROM Report r WHERE r.user = :user ORDER BY r.createdAt DESC")
    List<Report> findLatestReportByUserList(@Param("user") User user);
    /**
     * Helper method to get the first result
     */
    default Optional<Report> findLatestReportByUser(User user) {
        List<Report> reports = findLatestReportByUserList(user);
        return reports.isEmpty() ? Optional.empty() : Optional.of(reports.get(0));
    }
    // ==================== SPECIALIZED REPORT QUERIES ====================
    /**
     * Find regulatory reports
     */
    @Query("SELECT r FROM Report r WHERE r.type IN (:regulatoryTypes) ORDER BY r.createdAt DESC")
    List<Report> findRegulatoryReports(@Param("regulatoryTypes") List<ReportType> regulatoryTypes);
    /**
     * Helper method to call with proper enum values
     */
    default List<Report> findRegulatoryReports() {
        List<ReportType> regulatoryTypes = List.of(
                ReportType.COMPLIANCE_REPORT,
                ReportType.REGULATORY_FILING,
                ReportType.AUDIT_TRAIL
        );
        return findRegulatoryReports(regulatoryTypes);
    }
    /**
     * Find risk-related reports
     */
    @Query("SELECT r FROM Report r WHERE r.type IN (:riskTypes) ORDER BY r.createdAt DESC")
    List<Report> findRiskRelatedReports(@Param("riskTypes") List<ReportType> riskTypes);
    /**
     * Helper method to call with proper enum values
     */
    default List<Report> findRiskRelatedReports() {
        List<ReportType> riskTypes = List.of(
                ReportType.RISK_ANALYSIS,
                ReportType.MARGIN_REPORT,
                ReportType.EXPOSURE_REPORT,
                ReportType.STRESS_TEST_REPORT
        );
        return findRiskRelatedReports(riskTypes);
    }
    /**
     * Find portfolio-related reports
     */
    @Query("SELECT r FROM Report r WHERE r.type IN (:portfolioTypes) ORDER BY r.createdAt DESC")
    List<Report> findPortfolioRelatedReports(@Param("portfolioTypes") List<ReportType> portfolioTypes);
    /**
     * Helper method to call with proper enum values
     */
    default List<Report> findPortfolioRelatedReports() {
        List<ReportType> portfolioTypes = List.of(
                ReportType.PORTFOLIO_SUMMARY,
                ReportType.PERFORMANCE_REPORT,
                ReportType.VALUATION_REPORT
        );
        return findPortfolioRelatedReports(portfolioTypes);
    }
    // ==================== PERFORMANCE AND MONITORING QUERIES ====================
    /**
     * Find reports that took longer than 1 hour to generate - Using native query for MySQL
     */
    @Query(value = "SELECT * FROM report r WHERE r.generated_at IS NOT NULL AND " +
            "TIMESTAMPDIFF(HOUR, r.created_at, r.generated_at) > 1 " +
            "ORDER BY r.created_at DESC",
            nativeQuery = true)
    List<Report> findSlowGeneratedReports();
    /**
     * Find reports with generation errors (created but never generated and older than 24 hours)
     */
    @Query("SELECT r FROM Report r WHERE r.generatedAt IS NULL AND r.createdAt < :threshold")
    List<Report> findStaleReports(@Param("threshold") LocalDateTime threshold);
    // ==================== SEARCH QUERIES ====================
    /**
     * Search reports by name containing text
     */
    @Query("SELECT r FROM Report r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY r.createdAt DESC")
    List<Report> findByNameContainingIgnoreCase(@Param("searchTerm") String searchTerm);
    /**
     * Search reports by description containing text
     */
    @Query("SELECT r FROM Report r WHERE LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY r.createdAt DESC")
    List<Report> findByDescriptionContainingIgnoreCase(@Param("searchTerm") String searchTerm);
    /**
     * Search reports by name or description containing text
     */
    @Query("SELECT r FROM Report r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY r.createdAt DESC")
    List<Report> findByNameOrDescriptionContainingIgnoreCase(@Param("searchTerm") String searchTerm);
    // ==================== ADMIN QUERIES ====================
    /**
     * Find all reports with pagination for admin view
     */
    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);
    /**
     * Find reports by multiple types
     */
    @Query("SELECT r FROM Report r WHERE r.type IN :types ORDER BY r.createdAt DESC")
    List<Report> findByTypeIn(@Param("types") List<ReportType> types);
    /**
     * Find reports by user and date range
     */
    @Query("SELECT r FROM Report r WHERE r.user = :user AND r.createdAt BETWEEN :startDate AND :endDate ORDER BY r.createdAt DESC")
    List<Report> findByUserAndCreatedAtBetween(@Param("user") User user,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);
    // ==================== ANALYTICS QUERIES ====================
    /**
     * Get report generation success rate
     */
    @Query("SELECT " +
            "COUNT(r) as total, " +
            "SUM(CASE WHEN r.generatedAt IS NOT NULL THEN 1 ELSE 0 END) as generated " +
            "FROM Report r WHERE r.createdAt >= :date")
    Object[] getReportGenerationStats(@Param("date") LocalDateTime date);
    /**
     * Get average generation time for reports in minutes - Using native query for MySQL compatibility
     */
    @Query(value = "SELECT AVG(TIMESTAMPDIFF(MINUTE, created_at, generated_at)) " +
            "FROM report WHERE generated_at IS NOT NULL AND created_at >= :date",
            nativeQuery = true)
    Double getAverageGenerationTimeInMinutes(@Param("date") LocalDateTime date);
    /**
     * Get most popular report types
     */
    @Query("SELECT r.type, COUNT(r) as count FROM Report r WHERE r.createdAt >= :date GROUP BY r.type ORDER BY count DESC")
    List<Object[]> getMostPopularReportTypes(@Param("date") LocalDateTime date);
    /**
     * Get user activity (reports created per user)
     */
    @Query("SELECT r.user.username, r.user.firstName, r.user.lastName, COUNT(r) as reportCount " +
            "FROM Report r WHERE r.createdAt >= :date GROUP BY r.user.id, r.user.username, r.user.firstName, r.user.lastName " +
            "ORDER BY reportCount DESC")
    List<Object[]> getUserReportActivity(@Param("date") LocalDateTime date);
    // ==================== CLEANUP QUERIES ====================
    /**
     * Find old reports for cleanup (older than specified days)
     */
    @Query("SELECT r FROM Report r WHERE r.createdAt < :cutoffDate")
    List<Report> findOldReports(@Param("cutoffDate") LocalDateTime cutoffDate);
    /**
     * Count reports older than specified date for cleanup planning
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.createdAt < :cutoffDate")
    Long countOldReports(@Param("cutoffDate") LocalDateTime cutoffDate);
    // ==================== ADDITIONAL PERFORMANCE ANALYTICS ====================
    /**
     * Get generation time statistics for reports - Using native query
     */
    @Query(value = "SELECT " +
            "MIN(TIMESTAMPDIFF(MINUTE, created_at, generated_at)) as minTime, " +
            "MAX(TIMESTAMPDIFF(MINUTE, created_at, generated_at)) as maxTime, " +
            "AVG(TIMESTAMPDIFF(MINUTE, created_at, generated_at)) as avgTime " +
            "FROM report WHERE generated_at IS NOT NULL AND created_at >= :date",
            nativeQuery = true)
    Object[] getGenerationTimeStatistics(@Param("date") LocalDateTime date);
    /**
     * Find reports by generation time range - Using native query
     */
    @Query(value = "SELECT * FROM report r WHERE r.generated_at IS NOT NULL AND " +
            "TIMESTAMPDIFF(MINUTE, r.created_at, r.generated_at) BETWEEN :minMinutes AND :maxMinutes " +
            "ORDER BY r.created_at DESC",
            nativeQuery = true)
    List<Report> findReportsByGenerationTimeRange(@Param("minMinutes") Integer minMinutes,
                                                  @Param("maxMinutes") Integer maxMinutes);
    /**
     * Get daily report generation count for the past period - Using native query
     */
    @Query(value = "SELECT DATE(created_at) as reportDate, COUNT(*) as reportCount " +
            "FROM report WHERE created_at >= :startDate " +
            "GROUP BY DATE(created_at) ORDER BY reportDate DESC",
            nativeQuery = true)
    List<Object[]> getDailyReportCounts(@Param("startDate") LocalDateTime startDate);
    /**
     * Get hourly report generation pattern - Using native query
     */
    @Query(value = "SELECT HOUR(created_at) as reportHour, COUNT(*) as reportCount " +
            "FROM report WHERE created_at >= :startDate " +
            "GROUP BY HOUR(created_at) ORDER BY reportHour",
            nativeQuery = true)
    List<Object[]> getHourlyReportPattern(@Param("startDate") LocalDateTime startDate);
    // ==================== ALTERNATIVE SIMPLE QUERIES (NO COMPLEX DATE MATH) ====================
    /**
     * Simple alternative: Get all generated reports for manual calculation in service layer
     */
    @Query("SELECT r FROM Report r WHERE r.generatedAt IS NOT NULL AND r.createdAt >= :date ORDER BY r.createdAt DESC")
    List<Report> findGeneratedReportsAfterDate(@Param("date") LocalDateTime date);
    /**
     * Get report count by status for dashboard
     */
    @Query("SELECT " +
            "SUM(CASE WHEN r.generatedAt IS NULL THEN 1 ELSE 0 END) as pending, " +
            "SUM(CASE WHEN r.generatedAt IS NOT NULL THEN 1 ELSE 0 END) as completed " +
            "FROM Report r WHERE r.createdAt >= :date")
    Object[] getReportStatusCounts(@Param("date") LocalDateTime date);
}