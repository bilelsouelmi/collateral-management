// AlertController.java - Complete version using DTO methods to avoid lazy loading
package com.vermeg.collateralmanagement.controller;

import com.vermeg.collateralmanagement.entity.Alert;
import com.vermeg.collateralmanagement.entity.User;
import com.vermeg.collateralmanagement.enums.AlertType;
import com.vermeg.collateralmanagement.enums.AlertSeverity;
import com.vermeg.collateralmanagement.service.AlertService;
import com.vermeg.collateralmanagement.repository.UserRepository;
import com.vermeg.collateralmanagement.repository.AlertRepository;
import com.vermeg.collateralmanagement.dto.response.AlertResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertService alertService;
    private final UserRepository userRepository;
    private final AlertRepository alertRepository;

    /**
     * Get user alerts with optional filtering - FIXED: Uses DTO method
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<List<AlertResponse>> getUserAlerts(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                                             Authentication authentication) {
        log.info("User {} requesting alerts (unread only: {})", authentication.getName(), unreadOnly);

        Long currentUserId = getCurrentUserId(authentication);
        List<AlertResponse> alertResponses = alertService.getUserAlertsAsDto(currentUserId, unreadOnly);

        return ResponseEntity.ok(alertResponses);
    }

    /**
     * Get specific alert details - FIXED: Uses DTO method
     */
    @GetMapping("/{alertId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> getAlertDetails(@PathVariable Long alertId,
                                             Authentication authentication) {
        log.info("User {} requesting alert details: {}", authentication.getName(), alertId);

        try {
            Long currentUserId = getCurrentUserId(authentication);
            AlertResponse alertResponse = alertService.getAlertDetailsAsDto(alertId, currentUserId);

            return ResponseEntity.ok(alertResponse);
        } catch (Exception e) {
            log.error("Failed to get alert details: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to get alert: " + e.getMessage());
        }
    }

    /**
     * Mark specific alert as read - FIXED: Uses DTO method
     */
    @PostMapping("/{alertId}/read")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> markAlertAsRead(@PathVariable Long alertId,
                                             Authentication authentication) {
        log.info("User {} marking alert {} as read", authentication.getName(), alertId);

        try {
            Long currentUserId = getCurrentUserId(authentication);
            AlertResponse alertResponse = alertService.markAlertAsReadAndReturnDto(alertId, currentUserId);

            return ResponseEntity.ok(alertResponse);
        } catch (Exception e) {
            log.error("Failed to mark alert as read: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to mark alert as read: " + e.getMessage());
        }
    }

    /**
     * Mark all user alerts as read
     */
    @PostMapping("/read-all")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<String> markAllAlertsAsRead(Authentication authentication) {
        log.info("User {} marking all alerts as read", authentication.getName());

        Long currentUserId = getCurrentUserId(authentication);
        alertService.markAllAlertsAsRead(currentUserId);
        return ResponseEntity.ok("All alerts marked as read");
    }

    /**
     * Get unread alert count
     */
    @GetMapping("/count/unread")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<Long> getUnreadAlertCount(Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        long count = alertService.getUnreadAlertCount(currentUserId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get critical alerts for current user - FIXED: Uses DTO method
     */
    @GetMapping("/critical")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<List<AlertResponse>> getCriticalAlerts(Authentication authentication) {
        log.info("User {} requesting critical alerts", authentication.getName());

        Long currentUserId = getCurrentUserId(authentication);
        List<AlertResponse> alertResponses = alertService.getCriticalAlertsAsDto(currentUserId);

        return ResponseEntity.ok(alertResponses);
    }

    /**
     * Get alerts by type - FIXED: Uses DTO method
     */
    @GetMapping("/type/{alertType}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<List<AlertResponse>> getAlertsByType(@PathVariable AlertType alertType,
                                                               Authentication authentication) {
        log.info("User {} requesting alerts of type: {}", authentication.getName(), alertType);

        Long currentUserId = getCurrentUserId(authentication);
        List<AlertResponse> alertResponses = alertService.getAlertsByTypeAsDto(currentUserId, alertType);

        return ResponseEntity.ok(alertResponses);
    }

    /**
     * Get alerts by severity level - FIXED: Uses DTO method
     */
    @GetMapping("/severity/{severity}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<List<AlertResponse>> getAlertsBySeverity(@PathVariable AlertSeverity severity,
                                                                   Authentication authentication) {
        log.info("User {} requesting alerts of severity: {}", authentication.getName(), severity);

        Long currentUserId = getCurrentUserId(authentication);
        List<AlertResponse> alertResponses = alertService.getAlertsBySeverityAsDto(currentUserId, severity);

        return ResponseEntity.ok(alertResponses);
    }

    /**
     * Get alert statistics for current user
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<AlertService.AlertStatistics> getAlertStatistics(Authentication authentication) {
        log.info("User {} requesting alert statistics", authentication.getName());

        Long currentUserId = getCurrentUserId(authentication);
        AlertService.AlertStatistics stats = alertService.getAlertStatistics(currentUserId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Create test alerts (for development only)
     */
    @PostMapping("/test/create")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<String> createTestAlerts(Authentication authentication) {
        log.info("Creating test alerts for user: {}", authentication.getName());

        try {
            Long currentUserId = getCurrentUserId(authentication);
            User user = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Create test alerts directly using Alert entity
            Alert alert1 = Alert.builder()
                    .user(user)
                    .type(AlertType.MARGIN_CALL)
                    .severity(AlertSeverity.HIGH)
                    .title("Margin Call Required - Portfolio ABC")
                    .message("Your portfolio ABC has a margin shortfall of $50,000. Please add additional collateral or reduce exposure to meet margin requirements.")
                    .triggeredAt(LocalDateTime.now())
                    .isRead(false)
                    .build();

            Alert alert2 = Alert.builder()
                    .user(user)
                    .type(AlertType.THRESHOLD_BREACH)
                    .severity(AlertSeverity.MEDIUM)
                    .title("Risk Threshold Exceeded - Portfolio XYZ")
                    .message("Your portfolio XYZ has exceeded risk thresholds with a risk score of 0.85. Please review your positions and consider rebalancing.")
                    .triggeredAt(LocalDateTime.now())
                    .isRead(false)
                    .build();

            Alert alert3 = Alert.builder()
                    .user(user)
                    .type(AlertType.ASSET_MATURITY)
                    .severity(AlertSeverity.MEDIUM)
                    .title("Asset Nearing Maturity - Bond ABC123")
                    .message("Asset 'Government Bond ABC123' will mature in 7 days. Please plan for reinvestment or replacement to maintain portfolio positions.")
                    .triggeredAt(LocalDateTime.now())
                    .isRead(false)
                    .build();

            Alert alert4 = Alert.builder()
                    .user(user)
                    .type(AlertType.SYSTEM_ERROR)
                    .severity(AlertSeverity.CRITICAL)
                    .title("System Maintenance Alert")
                    .message("Scheduled system maintenance will occur tonight at 2 AM EST. Some features may be temporarily unavailable.")
                    .triggeredAt(LocalDateTime.now())
                    .isRead(true) // This one is already read
                    .build();

            Alert alert5 = Alert.builder()
                    .user(user)
                    .type(AlertType.PORTFOLIO_LIMIT)
                    .severity(AlertSeverity.HIGH)
                    .title("Concentration Limit Exceeded - Asset DEF456")
                    .message("Asset 'Corporate Bond DEF456' represents 45% of total portfolio value, which exceeds concentration limits. Consider diversifying your holdings.")
                    .triggeredAt(LocalDateTime.now())
                    .isRead(false)
                    .build();

            Alert alert6 = Alert.builder()
                    .user(user)
                    .type(AlertType.VALUATION_STALE)
                    .severity(AlertSeverity.LOW)
                    .title("Stale Valuation - Asset GHI789")
                    .message("Asset 'Municipal Bond GHI789' has not been revalued recently. Current valuations may be outdated.")
                    .triggeredAt(LocalDateTime.now())
                    .isRead(false)
                    .build();

            Alert alert7 = Alert.builder()
                    .user(user)
                    .type(AlertType.COMPLIANCE_VIOLATION)
                    .severity(AlertSeverity.CRITICAL)
                    .title("Compliance Violation Detected")
                    .message("A potential compliance violation has been detected in your trading activities. Please contact the compliance team immediately.")
                    .triggeredAt(LocalDateTime.now())
                    .isRead(false)
                    .build();

            Alert alert8 = Alert.builder()
                    .user(user)
                    .type(AlertType.MARKET_VOLATILITY)
                    .severity(AlertSeverity.MEDIUM)
                    .title("High Market Volatility Alert")
                    .message("Current market conditions show increased volatility. Consider reviewing your risk exposure and hedging strategies.")
                    .triggeredAt(LocalDateTime.now())
                    .isRead(false)
                    .build();

            // Save all alerts using your AlertRepository
            alertRepository.save(alert1);
            alertRepository.save(alert2);
            alertRepository.save(alert3);
            alertRepository.save(alert4);
            alertRepository.save(alert5);
            alertRepository.save(alert6);
            alertRepository.save(alert7);
            alertRepository.save(alert8);

            log.info("Successfully created 8 test alerts for user: {}", user.getUsername());
            return ResponseEntity.ok("8 test alerts created successfully");

        } catch (Exception e) {
            log.error("Failed to create test alerts: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to create test alerts: " + e.getMessage());
        }
    }

    /**
     * Delete old alerts (Admin function)
     */
    @DeleteMapping("/cleanup/{daysToKeep}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<String> deleteOldAlerts(@PathVariable int daysToKeep,
                                                  Authentication authentication) {
        log.info("Admin {} cleaning up alerts older than {} days", authentication.getName(), daysToKeep);

        try {
            alertService.deleteOldAlerts(daysToKeep);
            return ResponseEntity.ok("Old alerts cleanup completed");
        } catch (Exception e) {
            log.error("Failed to cleanup old alerts: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to cleanup alerts: " + e.getMessage());
        }
    }

    /**
     * Get current user ID from authentication using UserRepository
     */
    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return user.getId();
    }
}