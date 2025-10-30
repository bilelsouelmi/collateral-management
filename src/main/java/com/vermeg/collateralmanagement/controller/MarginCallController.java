package com.vermeg.collateralmanagement.controller;

import com.vermeg.collateralmanagement.dto.response.MarginCallResponse;
import com.vermeg.collateralmanagement.entity.MarginCall;
import com.vermeg.collateralmanagement.repository.MarginCallRepository;
import com.vermeg.collateralmanagement.service.MarginCallService;
import com.vermeg.collateralmanagement.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/margin-calls")
@CrossOrigin(origins = "*")
public class MarginCallController {

    private static final Logger log = LoggerFactory.getLogger(MarginCallController.class);

    @Autowired
    private MarginCallService marginCallService;

    @Autowired
    private MarginCallRepository marginCallRepository;

    /**
     * Calculate margin requirements for a portfolio
     */
    @GetMapping("/calculate/{portfolioId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> calculateMarginRequirements(@PathVariable Long portfolioId,
                                                         @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} calculating margin requirements for portfolio: {}",
                currentUser.getUsername(), portfolioId);

        try {
            BigDecimal marginRequirement = marginCallService.calculatePortfolioMarginRequirement(portfolioId);
            return ResponseEntity.ok(new MarginCalculationResponse(portfolioId, marginRequirement));
        } catch (Exception e) {
            log.error("Failed to calculate margin requirements: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to calculate margin: " + e.getMessage());
        }
    }

    /**
     * Monitor exposure limits for current user
     */
    @PostMapping("/monitor-exposure")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> monitorExposureLimits(@AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} initiating exposure limit monitoring", currentUser.getUsername());

        try {
            marginCallService.monitorExposureLimits(currentUser.getId());
            return ResponseEntity.ok("Exposure monitoring completed successfully");
        } catch (Exception e) {
            log.error("Failed to monitor exposure limits: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to monitor exposure: " + e.getMessage());
        }
    }

    /**
     * Get all active margin calls for current user
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<List<MarginCallResponse>> getActiveMarginCalls(@AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting active margin calls", currentUser.getUsername());

        try {
            // Use repository directly to get all margin calls for now
            List<MarginCall> marginCalls = marginCallRepository.findAll();

            List<MarginCallResponse> response = marginCalls.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get active margin calls: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get specific margin call details
     */
    @GetMapping("/{marginCallId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> getMarginCallDetails(@PathVariable Long marginCallId,
                                                  @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting margin call details: {}", currentUser.getUsername(), marginCallId);

        try {
            MarginCall marginCall = marginCallService.getMarginCallDetails(marginCallId, currentUser.getId());
            MarginCallResponse response = convertToResponse(marginCall);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get margin call details: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to get margin call: " + e.getMessage());
        }
    }

    /**
     * Acknowledge a margin call
     */
    @PostMapping("/{marginCallId}/acknowledge")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> acknowledgeMarginCall(@PathVariable Long marginCallId,
                                                   @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} acknowledging margin call: {}", currentUser.getUsername(), marginCallId);

        try {
            MarginCall marginCall = marginCallService.acknowledgeMarginCall(marginCallId, currentUser.getId());
            MarginCallResponse response = convertToResponse(marginCall);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to acknowledge margin call: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to acknowledge margin call: " + e.getMessage());
        }
    }

    /**
     * Resolve a margin call
     */
    @PostMapping("/{marginCallId}/resolve")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> resolveMarginCall(@PathVariable Long marginCallId,
                                               @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} resolving margin call: {}", currentUser.getUsername(), marginCallId);

        try {
            MarginCall marginCall = marginCallService.resolveMarginCall(marginCallId, currentUser.getId());
            MarginCallResponse response = convertToResponse(marginCall);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to resolve margin call: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to resolve margin call: " + e.getMessage());
        }
    }

    /**
     * Get overdue margin calls (Admin only)
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<List<MarginCallResponse>> getOverdueMarginCalls(@AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("Admin {} requesting overdue margin calls", currentUser.getUsername());

        try {
            List<MarginCall> overdueMarginCalls = marginCallService.getOverdueMarginCalls();
            List<MarginCallResponse> response = overdueMarginCalls.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get overdue margin calls: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Convert MarginCall entity to DTO response
     * Safely handles lazy loading to avoid Jackson serialization issues
     */
    private MarginCallResponse convertToResponse(MarginCall marginCall) {
        return MarginCallResponse.builder()
                .id(marginCall.getId())
                .requiredMargin(marginCall.getRequiredMargin())
                .currentMargin(marginCall.getCurrentMargin())
                .shortfall(marginCall.getShortfall())
                .dueDate(marginCall.getDueDate())
                .status(marginCall.getStatus().name())
                .createdAt(marginCall.getCreatedAt())
                .lastModifiedAt(marginCall.getLastModifiedAt())
                .portfolio(convertPortfolioInfo(marginCall))
                .build();
    }

    /**
     * Convert Portfolio entity to DTO safely
     */
    private MarginCallResponse.PortfolioInfo convertPortfolioInfo(MarginCall marginCall) {
        if (marginCall.getPortfolio() == null) {
            return null;
        }

        return MarginCallResponse.PortfolioInfo.builder()
                .id(marginCall.getPortfolio().getId())
                .name(marginCall.getPortfolio().getName())
                .user(MarginCallResponse.UserInfo.builder()
                        .id(1L) // Default user ID to avoid lazy loading issues
                        .username("system")
                        .fullName("System User")
                        .build())
                .build();
    }

    // Response DTO for margin calculation
    public static class MarginCalculationResponse {
        public final Long portfolioId;
        public final BigDecimal marginRequirement;

        public MarginCalculationResponse(Long portfolioId, BigDecimal marginRequirement) {
            this.portfolioId = portfolioId;
            this.marginRequirement = marginRequirement;
        }

        public Long getPortfolioId() {
            return portfolioId;
        }

        public BigDecimal getMarginRequirement() {
            return marginRequirement;
        }
    }
}