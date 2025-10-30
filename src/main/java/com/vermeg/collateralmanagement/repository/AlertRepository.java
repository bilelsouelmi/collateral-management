package com.vermeg.collateralmanagement.repository;

import com.vermeg.collateralmanagement.entity.Alert;
import com.vermeg.collateralmanagement.enums.AlertType;
import com.vermeg.collateralmanagement.enums.AlertSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * Find alerts by user ID ordered by creation date
     */
    List<Alert> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find unread alerts by user ID
     */
    List<Alert> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * Find unread alerts by user ID (simple)
     */
    List<Alert> findByUserIdAndIsReadFalse(Long userId);

    /**
     * Find alerts by user ID and severity
     */
    List<Alert> findByUserIdAndSeverityOrderByCreatedAtDesc(Long userId, AlertSeverity severity);

    /**
     * Find alerts by user ID and type
     */
    List<Alert> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, AlertType type);

    /**
     * Find alerts by severity
     */
    List<Alert> findBySeverityOrderByCreatedAtDesc(AlertSeverity severity);

    /**
     * Find alerts by type
     */
    List<Alert> findByTypeOrderByCreatedAtDesc(AlertType type);

    /**
     * Find alerts created before date and already read
     */
    List<Alert> findByCreatedAtBeforeAndIsReadTrue(LocalDateTime cutoffDate);

    /**
     * Count total alerts by user
     */
    long countByUserId(Long userId);

    /**
     * Count unread alerts by user
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * Count alerts by user and severity
     */
    long countByUserIdAndSeverity(Long userId, AlertSeverity severity);

    /**
     * Count alerts by user and type
     */
    long countByUserIdAndType(Long userId, AlertType type);

    /**
     * Find recent alerts (within specified hours)
     */
    @Query("SELECT a FROM Alert a WHERE a.createdAt > :cutoffDate ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlerts(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find alerts by user and date range
     */
    @Query("SELECT a FROM Alert a WHERE a.user.id = :userId AND a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<Alert> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    /**
     * Find critical unread alerts - FIXED: Use proper enum value
     */
    @Query("SELECT a FROM Alert a WHERE a.severity = :criticalSeverity AND a.isRead = false ORDER BY a.createdAt DESC")
    List<Alert> findCriticalUnreadAlerts(@Param("criticalSeverity") AlertSeverity criticalSeverity);

    // Helper method to use the above query
    default List<Alert> findCriticalUnreadAlerts() {
        return findCriticalUnreadAlerts(AlertSeverity.CRITICAL);
    }

    /**
     * Find alerts requiring immediate attention - FIXED: Use parameters
     */
    @Query("SELECT a FROM Alert a WHERE a.severity IN (:severities) AND a.isRead = false ORDER BY a.severity DESC, a.createdAt DESC")
    List<Alert> findAlertsRequiringAttention(@Param("severities") List<AlertSeverity> severities);

    // Helper method to use the above query
    default List<Alert> findAlertsRequiringAttention() {
        return findAlertsRequiringAttention(List.of(AlertSeverity.CRITICAL, AlertSeverity.HIGH));
    }

    /**
     * Find alerts by multiple criteria
     */
    @Query("SELECT a FROM Alert a WHERE " +
            "(:userId IS NULL OR a.user.id = :userId) AND " +
            "(:type IS NULL OR a.type = :type) AND " +
            "(:severity IS NULL OR a.severity = :severity) AND " +
            "(:isRead IS NULL OR a.isRead = :isRead) " +
            "ORDER BY a.createdAt DESC")
    List<Alert> findByMultipleCriteria(@Param("userId") Long userId,
                                       @Param("type") AlertType type,
                                       @Param("severity") AlertSeverity severity,
                                       @Param("isRead") Boolean isRead);

    /**
     * Get alert statistics by type
     */
    @Query("SELECT a.type, COUNT(a), SUM(CASE WHEN a.isRead = false THEN 1 ELSE 0 END) FROM Alert a GROUP BY a.type")
    List<Object[]> getAlertStatisticsByType();

    /**
     * Get alert statistics by severity
     */
    @Query("SELECT a.severity, COUNT(a), SUM(CASE WHEN a.isRead = false THEN 1 ELSE 0 END) FROM Alert a GROUP BY a.severity")
    List<Object[]> getAlertStatisticsBySeverity();

    /**
     * Find alerts triggered in date range
     */
    @Query("SELECT a FROM Alert a WHERE a.triggeredAt BETWEEN :startDate AND :endDate ORDER BY a.triggeredAt DESC")
    List<Alert> findByTriggeredAtBetween(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Find overdue alerts (not read within specified time) - FIXED: Use parameters
     */
    @Query("SELECT a FROM Alert a WHERE a.severity IN (:severities) AND a.isRead = false AND a.createdAt < :overdueDate")
    List<Alert> findOverdueAlerts(@Param("severities") List<AlertSeverity> severities, @Param("overdueDate") LocalDateTime overdueDate);

    // Helper method to use the above query
    default List<Alert> findOverdueAlerts(LocalDateTime overdueDate) {
        return findOverdueAlerts(List.of(AlertSeverity.CRITICAL, AlertSeverity.HIGH), overdueDate);
    }

    /**
     * Find duplicate alerts (same user, type, and recent)
     */
    @Query("SELECT a FROM Alert a WHERE a.user.id = :userId AND a.type = :type AND a.createdAt > :recentDate")
    List<Alert> findDuplicateAlerts(@Param("userId") Long userId,
                                    @Param("type") AlertType type,
                                    @Param("recentDate") LocalDateTime recentDate);

    /**
     * Find alerts by user and severity level or higher - SIMPLIFIED: Use business logic in service
     */
    @Query("SELECT a FROM Alert a WHERE a.user.id = :userId AND a.severity IN (:severities) ORDER BY a.createdAt DESC")
    List<Alert> findByUserIdAndSeverityIn(@Param("userId") Long userId, @Param("severities") List<AlertSeverity> severities);

    // Helper methods for different severity levels
    default List<Alert> findByUserIdAndSeverityLevelOrHigher(Long userId, AlertSeverity severity) {
        List<AlertSeverity> severities = switch (severity) {
            case LOW -> List.of(AlertSeverity.LOW, AlertSeverity.MEDIUM, AlertSeverity.HIGH, AlertSeverity.CRITICAL);
            case MEDIUM -> List.of(AlertSeverity.MEDIUM, AlertSeverity.HIGH, AlertSeverity.CRITICAL);
            case HIGH -> List.of(AlertSeverity.HIGH, AlertSeverity.CRITICAL);
            case CRITICAL -> List.of(AlertSeverity.CRITICAL);
        };
        return findByUserIdAndSeverityIn(userId, severities);
    }

    /**
     * Get daily alert counts for user - FIXED: Use native query for DATE function
     */
    @Query(value = "SELECT DATE(created_at) as alertDate, COUNT(*) as alertCount " +
            "FROM alerts WHERE user_id = :userId AND created_at >= :startDate " +
            "GROUP BY DATE(created_at) ORDER BY alertDate DESC",
            nativeQuery = true)
    List<Object[]> getDailyAlertCounts(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    /**
     * Find system-wide critical alerts - FIXED: Use parameter
     */
    @Query("SELECT a FROM Alert a WHERE a.severity = :severity ORDER BY a.createdAt DESC")
    List<Alert> findSystemWideAlertsBySeverity(@Param("severity") AlertSeverity severity);

    // Helper method
    default List<Alert> findSystemWideCriticalAlerts() {
        return findSystemWideAlertsBySeverity(AlertSeverity.CRITICAL);
    }

    /**
     * Check if user has recent alert of specific type
     */
    @Query("SELECT COUNT(a) > 0 FROM Alert a WHERE a.user.id = :userId AND a.type = :type AND a.createdAt > :recentDate")
    boolean hasRecentAlertOfType(@Param("userId") Long userId, @Param("type") AlertType type, @Param("recentDate") LocalDateTime recentDate);

    // ==================== NEW METHODS FOR PHASE 5.1 ====================

    /**
     * Find alerts created after specific date
     */
    @Query("SELECT a FROM Alert a WHERE a.createdAt >= :date ORDER BY a.createdAt DESC")
    List<Alert> findAlertsCreatedAfter(@Param("date") LocalDateTime date);

    /**
     * Get alert trend data for charts - Uses native query for database functions
     */
    @Query(value = "SELECT CAST(CONCAT(DATE(created_at), ' 00:00:00') AS DATETIME) as date, COUNT(*) as total, " +
            "SUM(CASE WHEN severity = 'CRITICAL' THEN 1 ELSE 0 END) as critical, " +
            "SUM(CASE WHEN is_read = false THEN 1 ELSE 0 END) as unread " +
            "FROM alerts WHERE created_at >= :startDate AND user_id = :userId " +
            "GROUP BY DATE(created_at) ORDER BY date DESC",
            nativeQuery = true)
    List<Object[]> getAlertTrendsByUser(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    /**
     * Get alert summary statistics by user - Uses native query for CASE statements
     */
    @Query(value = "SELECT " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN severity = 'CRITICAL' THEN 1 ELSE 0 END) as critical, " +
            "SUM(CASE WHEN severity = 'HIGH' THEN 1 ELSE 0 END) as high, " +
            "SUM(CASE WHEN severity = 'MEDIUM' THEN 1 ELSE 0 END) as medium, " +
            "SUM(CASE WHEN severity = 'LOW' THEN 1 ELSE 0 END) as low, " +
            "SUM(CASE WHEN is_read = false THEN 1 ELSE 0 END) as unread " +
            "FROM alerts WHERE user_id = :userId AND created_at >= :since",
            nativeQuery = true)
    List<Object[]> getAlertSummaryByUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}