// AlertService.java - Your original code with only compilation fixes
package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.entity.*;
import com.vermeg.collateralmanagement.enums.AlertType;
import com.vermeg.collateralmanagement.enums.AlertSeverity;
import com.vermeg.collateralmanagement.repository.AlertRepository;
import com.vermeg.collateralmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    /**
     * Create margin call alert
     */
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
                .type(AlertType.THRESHOLD_BREACH) // FIXED: Use correct enum value from your enum
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

    /**
     * Get user alerts with filtering
     */
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
    public Alert markAlertAsRead(Long alertId, Long userId) {
        log.info("User {} marking alert {} as read", userId, alertId);

        Alert alert = getAlertDetails(alertId, userId);
        alert.markAsRead();

        return alertRepository.save(alert);
    }

    /**
     * Mark all user alerts as read
     */
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
    public long getUnreadAlertCount(Long userId) {
        return alertRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Get critical alerts for user
     */
    public List<Alert> getCriticalAlerts(Long userId) {
        return alertRepository.findByUserIdAndSeverityOrderByCreatedAtDesc(userId, AlertSeverity.CRITICAL);
    }

    /**
     * Get alerts by type for user
     */
    public List<Alert> getAlertsByType(Long userId, AlertType alertType) {
        return alertRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, alertType);
    }

    /**
     * Delete old alerts (cleanup)
     */
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
    public AlertStatistics getAlertStatistics(Long userId) {
        long totalAlerts = alertRepository.countByUserId(userId);
        long unreadAlerts = alertRepository.countByUserIdAndIsReadFalse(userId);
        long criticalAlerts = alertRepository.countByUserIdAndSeverity(userId, AlertSeverity.CRITICAL);
        long highAlerts = alertRepository.countByUserIdAndSeverity(userId, AlertSeverity.HIGH);

        return new AlertStatistics(totalAlerts, unreadAlerts, criticalAlerts, highAlerts);
    }

    // Private helper methods
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

    // FIXED: Proper inner class declaration
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