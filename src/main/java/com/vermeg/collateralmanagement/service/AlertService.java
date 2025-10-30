// AlertService.java - Complete version with DTO conversion within transactions
package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.entity.*;
import com.vermeg.collateralmanagement.enums.AlertType;
import com.vermeg.collateralmanagement.enums.AlertSeverity;
import com.vermeg.collateralmanagement.repository.AlertRepository;
import com.vermeg.collateralmanagement.repository.UserRepository;
import com.vermeg.collateralmanagement.dto.response.AlertResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    /**
     * Create margin call alert
     */
    @Transactional
    public Alert createMarginAlert(User user, Portfolio portfolio, BigDecimal shortfall) {
        log.info("Creating margin alert for user: {} with shortfall: {}", user.getId(), shortfall);

        String title = "Margin Call Required - Portfolio: " + portfolio.getName();
        String message = String.format(
                "Your portfolio '%s' has a margin shortfall of %s. " +
                        "Please add additional collateral or reduce exposure to meet margin requirements.",
                portfolio.getName(), shortfall.toString());

        Alert alert = Alert.builder()
                .user(user)
                .type(AlertType.MARGIN_CALL)
                .severity(AlertSeverity.HIGH)
                .title(title)
                .message(message)
                .triggeredAt(LocalDateTime.now())
                .isRead(false)
                .build();

        return alertRepository.save(alert);
    }

    /**
     * Create risk threshold alert
     */
    @Transactional
    public Alert createRiskAlert(User user, Portfolio portfolio, RiskMetric riskMetric) {
        log.info("Creating risk alert for user: {} with risk score: {}",
                user.getId(), riskMetric.getValue());

        AlertSeverity severity = determineRiskSeverity(riskMetric.getValue());

        String title = "Risk Threshold Exceeded - Portfolio: " + portfolio.getName();
        String message = String.format(
                "Your portfolio '%s' has exceeded risk thresholds with a risk score of %.2f. " +
                        "Please review your positions and consider rebalancing.",
                portfolio.getName(), riskMetric.getValue());

        Alert alert = Alert.builder()
                .user(user)
                .type(AlertType.THRESHOLD_BREACH)
                .severity(severity)
                .title(title)
                .message(message)
                .triggeredAt(LocalDateTime.now())
                .isRead(false)
                .build();

        return alertRepository.save(alert);
    }

    /**
     * Create concentration risk alert
     */
    @Transactional
    public Alert createConcentrationAlert(User user, Portfolio portfolio,
                                          CollateralAsset asset, BigDecimal concentration) {
        log.info("Creating concentration alert for user: {} for asset: {} with concentration: {}",
                user.getId(), asset.getName(), concentration);

        String title = "Concentration Limit Exceeded - Asset: " + asset.getName();
        String message = String.format(
                "Asset '%s' in portfolio '%s' represents %.1f%% of total portfolio value, " +
                        "which exceeds concentration limits. Consider diversifying your holdings.",
                asset.getName(), portfolio.getName(),
                concentration.multiply(new BigDecimal("100")));

        Alert alert = Alert.builder()
                .user(user)
                .type(AlertType.PORTFOLIO_LIMIT)
                .severity(AlertSeverity.MEDIUM)
                .title(title)
                .message(message)
                .triggeredAt(LocalDateTime.now())
                .isRead(false)
                .build();

        return alertRepository.save(alert);
    }

    /**
     * Create asset maturity alert
     */
    @Transactional
    public Alert createMaturityAlert(User user, CollateralAsset asset, int daysToMaturity) {
        log.info("Creating maturity alert for user: {} for asset: {} with {} days to maturity",
                user.getId(), asset.getName(), daysToMaturity);

        AlertSeverity severity = daysToMaturity <= 7 ? AlertSeverity.HIGH : AlertSeverity.MEDIUM;

        String title = "Asset Nearing Maturity - " + asset.getName();
        String message = String.format(
                "Asset '%s' will mature in %d days. Please plan for reinvestment or " +
                        "replacement to maintain portfolio positions.",
                asset.getName(), daysToMaturity);

        Alert alert = Alert.builder()
                .user(user)
                .type(AlertType.ASSET_MATURITY)
                .severity(severity)
                .title(title)
                .message(message)
                .triggeredAt(LocalDateTime.now())
                .isRead(false)
                .build();

        return alertRepository.save(alert);
    }

    /**
     * Create valuation stale alert
     */
    @Transactional
    public Alert createStaleValuationAlert(User user, CollateralAsset asset) {
        log.info("Creating stale valuation alert for user: {} for asset: {}",
                user.getId(), asset.getName());

        String title = "Stale Valuation - " + asset.getName();
        String message = String.format(
                "Asset '%s' has not been revalued recently. Current valuations may be outdated. " +
                        "Please update asset pricing for accurate risk assessment.",
                asset.getName());

        Alert alert = Alert.builder()
                .user(user)
                .type(AlertType.VALUATION_STALE)
                .severity(AlertSeverity.LOW)
                .title(title)
                .message(message)
                .triggeredAt(LocalDateTime.now())
                .isRead(false)
                .build();

        return alertRepository.save(alert);
    }

    /**
     * Create system error alert
     */
    @Transactional
    public Alert createSystemAlert(User user, String errorMessage) {
        log.info("Creating system alert for user: {}", user.getId());

        String title = "System Error Alert";
        String message = "A system error has occurred: " + errorMessage +
                ". Please contact support if this issue persists.";

        Alert alert = Alert.builder()
                .user(user)
                .type(AlertType.SYSTEM_ERROR)
                .severity(AlertSeverity.CRITICAL)
                .title(title)
                .message(message)
                .triggeredAt(LocalDateTime.now())
                .isRead(false)
                .build();

        return alertRepository.save(alert);
    }

    // ==================== DTO METHODS (FIXED FOR LAZY LOADING) ====================

    /**
     * Get user alerts as DTOs to avoid lazy loading
     */
    @Transactional(readOnly = true)
    public List<AlertResponse> getUserAlertsAsDto(Long userId, boolean unreadOnly) {
        List<Alert> alerts;
        if (unreadOnly) {
            alerts = alertRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        } else {
            alerts = alertRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        // Convert to DTOs within the transaction while User is still accessible
        return alerts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get alert details as DTO
     */
    @Transactional(readOnly = true)
    public AlertResponse getAlertDetailsAsDto(Long alertId, Long userId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));

        if (!alert.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied: Alert belongs to another user");
        }

        return convertToDto(alert);
    }

    /**
     * Get critical alerts as DTOs
     */
    @Transactional(readOnly = true)
    public List<AlertResponse> getCriticalAlertsAsDto(Long userId) {
        List<Alert> alerts = alertRepository.findByUserIdAndSeverityOrderByCreatedAtDesc(userId, AlertSeverity.CRITICAL);
        return alerts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get alerts by type as DTOs
     */
    @Transactional(readOnly = true)
    public List<AlertResponse> getAlertsByTypeAsDto(Long userId, AlertType alertType) {
        List<Alert> alerts = alertRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, alertType);
        return alerts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get alerts by severity as DTOs
     */
    @Transactional(readOnly = true)
    public List<AlertResponse> getAlertsBySeverityAsDto(Long userId, AlertSeverity severity) {
        List<Alert> alerts = alertRepository.findByUserIdAndSeverityOrderByCreatedAtDesc(userId, severity);
        return alerts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Mark alert as read and return DTO
     */
    @Transactional
    public AlertResponse markAlertAsReadAndReturnDto(Long alertId, Long userId) {
        log.info("User {} marking alert {} as read", userId, alertId);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));

        if (!alert.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied: Alert belongs to another user");
        }

        alert.markAsRead();
        Alert savedAlert = alertRepository.save(alert);

        return convertToDto(savedAlert);
    }

    // ==================== LEGACY METHODS (KEEP FOR COMPATIBILITY) ====================

    /**
     * Get user alerts with filtering
     */
    @Transactional(readOnly = true)
    public List<Alert> getUserAlerts(Long userId, boolean unreadOnly) {
        if (unreadOnly) {
            return alertRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        } else {
            return alertRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
    }

    /**
     * Get alert details with access control
     */
    @Transactional(readOnly = true)
    public Alert getAlertDetails(Long alertId, Long userId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));

        if (!alert.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied: Alert belongs to another user");
        }

        return alert;
    }

    /**
     * Mark alert as read
     */
    @Transactional
    public Alert markAlertAsRead(Long alertId, Long userId) {
        log.info("User {} marking alert {} as read", userId, alertId);

        Alert alert = getAlertDetails(alertId, userId);
        alert.markAsRead();

        return alertRepository.save(alert);
    }

    /**
     * Mark all user alerts as read
     */
    @Transactional
    public void markAllAlertsAsRead(Long userId) {
        log.info("Marking all alerts as read for user: {}", userId);

        List<Alert> unreadAlerts = alertRepository.findByUserIdAndIsReadFalse(userId);

        for (Alert alert : unreadAlerts) {
            alert.markAsRead();
        }

        alertRepository.saveAll(unreadAlerts);
        log.info("Marked {} alerts as read for user: {}", unreadAlerts.size(), userId);
    }

    /**
     * Get unread alert count
     */
    @Transactional(readOnly = true)
    public long getUnreadAlertCount(Long userId) {
        return alertRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Get critical alerts for user
     */
    @Transactional(readOnly = true)
    public List<Alert> getCriticalAlerts(Long userId) {
        return alertRepository.findByUserIdAndSeverityOrderByCreatedAtDesc(userId, AlertSeverity.CRITICAL);
    }

    /**
     * Get alerts by type for user
     */
    @Transactional(readOnly = true)
    public List<Alert> getAlertsByType(Long userId, AlertType alertType) {
        return alertRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, alertType);
    }

    /**
     * Delete old alerts (cleanup)
     */
    @Transactional
    public void deleteOldAlerts(int daysToKeep) {
        log.info("Cleaning up alerts older than {} days", daysToKeep);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        List<Alert> oldAlerts = alertRepository.findByCreatedAtBeforeAndIsReadTrue(cutoffDate);

        alertRepository.deleteAll(oldAlerts);
        log.info("Deleted {} old alerts", oldAlerts.size());
    }

    /**
     * Get alert statistics
     */
    @Transactional(readOnly = true)
    public AlertStatistics getAlertStatistics(Long userId) {
        long totalAlerts = alertRepository.countByUserId(userId);
        long unreadAlerts = alertRepository.countByUserIdAndIsReadFalse(userId);
        long criticalAlerts = alertRepository.countByUserIdAndSeverity(userId, AlertSeverity.CRITICAL);
        long highAlerts = alertRepository.countByUserIdAndSeverity(userId, AlertSeverity.HIGH);

        return new AlertStatistics(totalAlerts, unreadAlerts, criticalAlerts, highAlerts);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Convert Alert entity to DTO within transaction
     */
    private AlertResponse convertToDto(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .type(alert.getType())
                .typeName(alert.getType().getDisplayName())
                .severity(alert.getSeverity())
                .severityName(alert.getSeverity().getDisplayName())
                .createdAt(alert.getCreatedAt())
                .triggeredAt(alert.getTriggeredAt())
                .isRead(alert.getIsRead())
                .isUnread(alert.isUnread())
                .isCritical(alert.isCritical())
                .requiresImmediateAttention(alert.requiresImmediateAttention())
                .userId(alert.getUser().getId())
                .userName(alert.getUser().getUsername())
                .userFullName(alert.getUser().getFirstName() + " " + alert.getUser().getLastName())
                .minutesSinceTriggered(alert.getMinutesSinceTriggered())
                .timeAgo(formatTimeAgo(alert.getMinutesSinceTriggered()))
                .build();
    }

    /**
     * Format time ago helper method
     */
    private String formatTimeAgo(long minutes) {
        if (minutes < 60) {
            return minutes + " minutes ago";
        } else if (minutes < 1440) { // 24 hours
            return (minutes / 60) + " hours ago";
        } else {
            return (minutes / 1440) + " days ago";
        }
    }

    /**
     * Determine risk severity based on score
     */
    private AlertSeverity determineRiskSeverity(BigDecimal riskScore) {
        if (riskScore.compareTo(new BigDecimal("0.9")) >= 0) {
            return AlertSeverity.CRITICAL;
        } else if (riskScore.compareTo(new BigDecimal("0.75")) >= 0) {
            return AlertSeverity.HIGH;
        } else if (riskScore.compareTo(new BigDecimal("0.5")) >= 0) {
            return AlertSeverity.MEDIUM;
        } else {
            return AlertSeverity.LOW;
        }
    }

    // ==================== INNER CLASSES ====================

    /**
     * AlertStatistics inner class
     */
    public static class AlertStatistics {
        public final long totalAlerts;
        public final long unreadAlerts;
        public final long criticalAlerts;
        public final long highAlerts;

        public AlertStatistics(long totalAlerts, long unreadAlerts, long criticalAlerts, long highAlerts) {
            this.totalAlerts = totalAlerts;
            this.unreadAlerts = unreadAlerts;
            this.criticalAlerts = criticalAlerts;
            this.highAlerts = highAlerts;
        }
    }
}