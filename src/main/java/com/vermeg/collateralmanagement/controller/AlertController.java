// AlertController.java - Updated with proper getCurrentUserId implementation
package com.vermeg.collateralmanagement.controller;

import com.vermeg.collateralmanagement.entity.Alert;
import com.vermeg.collateralmanagement.entity.User;
import com.vermeg.collateralmanagement.enums.AlertType;
import com.vermeg.collateralmanagement.enums.AlertSeverity;
import com.vermeg.collateralmanagement.service.AlertService;
import com.vermeg.collateralmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertService alertService;
    private final UserRepository userRepository;

    /**
     * Get user alerts with optional filtering
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<List<Alert>> getUserAlerts(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                                     Authentication authentication) {
        log.info("User {} requesting alerts (unread only: {})", authentication.getName(), unreadOnly);

        Long currentUserId = getCurrentUserId(authentication);
        List<Alert> alerts = alertService.getUserAlerts(currentUserId, unreadOnly);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get specific alert details
     */
    @GetMapping("/{alertId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> getAlertDetails(@PathVariable Long alertId,
                                             Authentication authentication) {
        log.info("User {} requesting alert details: {}", authentication.getName(), alertId);

        try {
            Long currentUserId = getCurrentUserId(authentication);
            Alert alert = alertService.getAlertDetails(alertId, currentUserId);
            return ResponseEntity.ok(alert);
        } catch (Exception e) {
            log.error("Failed to get alert details: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to get alert: " + e.getMessage());
        }
    }

    /**
     * Mark specific alert as read
     */
    @PostMapping("/{alertId}/read")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> markAlertAsRead(@PathVariable Long alertId,
                                             Authentication authentication) {
        log.info("User {} marking alert {} as read", authentication.getName(), alertId);

        try {
            Long currentUserId = getCurrentUserId(authentication);
            Alert alert = alertService.markAlertAsRead(alertId, currentUserId);
            return ResponseEntity.ok(alert);
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
     * Get critical alerts for current user
     */
    @GetMapping("/critical")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<List<Alert>> getCriticalAlerts(Authentication authentication) {
        log.info("User {} requesting critical alerts", authentication.getName());

        Long currentUserId = getCurrentUserId(authentication);
        List<Alert> criticalAlerts = alertService.getCriticalAlerts(currentUserId);
        return ResponseEntity.ok(criticalAlerts);
    }

    /**
     * Get alerts by type
     */
    @GetMapping("/type/{alertType}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<List<Alert>> getAlertsByType(@PathVariable AlertType alertType,
                                                       Authentication authentication) {
        log.info("User {} requesting alerts of type: {}", authentication.getName(), alertType);

        Long currentUserId = getCurrentUserId(authentication);
        List<Alert> alerts = alertService.getAlertsByType(currentUserId, alertType);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get alerts by severity level
     */
    @GetMapping("/severity/{severity}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<List<Alert>> getAlertsBySeverity(@PathVariable AlertSeverity severity,
                                                           Authentication authentication) {
        log.info("User {} requesting alerts of severity: {}", authentication.getName(), severity);

        Long currentUserId = getCurrentUserId(authentication);
        // Filter alerts by severity using the existing getUserAlerts method
        List<Alert> alerts = alertService.getUserAlerts(currentUserId, false)
                .stream()
                .filter(alert -> alert.getSeverity() == severity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(alerts);
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