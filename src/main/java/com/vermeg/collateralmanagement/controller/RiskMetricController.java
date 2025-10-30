package com.vermeg.collateralmanagement.controller;

import com.vermeg.collateralmanagement.dto.risk.*;
import com.vermeg.collateralmanagement.entity.RiskMetric;
import com.vermeg.collateralmanagement.service.MarginCallService;
import com.vermeg.collateralmanagement.service.RiskMetricService;
import com.vermeg.collateralmanagement.security.UserPrincipal;
import com.vermeg.collateralmanagement.service.RiskSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/risk-metrics")
@CrossOrigin(origins = "*")
public class RiskMetricController {

    private static final Logger log = LoggerFactory.getLogger(RiskMetricController.class);

    @Autowired
    private RiskMetricService riskMetricService;

    @Autowired
    private MarginCallService marginCallService;

    @Autowired
    private RiskSchedulerService riskSchedulerService;

    /**
     * Get risk overview for current user
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<RiskOverviewDto> getRiskOverview(@AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting risk overview", currentUser.getUsername());

        try {
            RiskOverviewDto overview = riskMetricService.getRiskOverview(currentUser.getId());
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            log.error("Failed to get risk overview: {}", e.getMessage());
            throw new RuntimeException("Failed to get risk overview: " + e.getMessage());
        }
    }

    /**
     * Get detailed risk analysis for a portfolio
     */
    @GetMapping("/portfolio/{portfolioId}/detail")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<PortfolioRiskDetailDto> getPortfolioRiskDetail(
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting risk detail for portfolio: {}",
                currentUser.getUsername(), portfolioId);

        try {
            PortfolioRiskDetailDto detail = riskMetricService.getPortfolioRiskDetail(
                    portfolioId, currentUser.getId());
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            log.error("Failed to get portfolio risk detail: {}", e.getMessage());
            throw new RuntimeException("Failed to get portfolio risk detail: " + e.getMessage());
        }
    }

    /**
     * Perform comprehensive risk assessment for a portfolio
     */
    @PostMapping("/assessment/{portfolioId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> performRiskAssessment(@PathVariable Long portfolioId,
                                                   @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} performing risk assessment for portfolio: {}",
                currentUser.getUsername(), portfolioId);

        try {
            marginCallService.performRiskAssessment(portfolioId);
            return ResponseEntity.ok("Risk assessment completed successfully");
        } catch (Exception e) {
            log.error("Failed to perform risk assessment: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to perform risk assessment: " + e.getMessage());
        }
    }

    /**
     * Calculate portfolio risk metrics
     */
    @PostMapping("/calculate/{portfolioId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> calculatePortfolioRisk(@PathVariable Long portfolioId,
                                                    @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} calculating risk for portfolio: {}", currentUser.getUsername(), portfolioId);

        try {
            RiskMetric riskMetric = riskMetricService.calculatePortfolioRisk(portfolioId, currentUser.getId());
            return ResponseEntity.ok(riskMetric);
        } catch (Exception e) {
            log.error("Failed to calculate portfolio risk: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to calculate risk: " + e.getMessage());
        }
    }

    /**
     * Get latest risk metric for a portfolio
     */
    @GetMapping("/latest/{portfolioId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> getLatestRiskMetric(@PathVariable Long portfolioId,
                                                 @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting latest risk metric for portfolio: {}",
                currentUser.getUsername(), portfolioId);

        try {
            RiskMetric riskMetric = riskMetricService.getLatestRiskMetric(portfolioId, currentUser.getId());
            if (riskMetric != null) {
                return ResponseEntity.ok(riskMetric);
            } else {
                return ResponseEntity.ok("No risk metrics found for portfolio");
            }
        } catch (Exception e) {
            log.error("Failed to get latest risk metric: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to get risk metric: " + e.getMessage());
        }
    }

    /**
     * Get risk metrics history for a portfolio
     */
    @GetMapping("/history/{portfolioId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> getRiskMetricsHistory(@PathVariable Long portfolioId,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                                   @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting risk metrics history for portfolio: {} from {} to {}",
                currentUser.getUsername(), portfolioId, startDate, endDate);

        try {
            List<RiskMetric> riskMetrics = riskMetricService.getRiskMetricsHistory(
                    portfolioId, currentUser.getId(), startDate, endDate);
            return ResponseEntity.ok(riskMetrics);
        } catch (Exception e) {
            log.error("Failed to get risk metrics history: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to get risk history: " + e.getMessage());
        }
    }

    /**
     * Get risk metrics summary for current user
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<List<RiskMetric>> getUserRiskMetrics(@AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting risk metrics summary", currentUser.getUsername());

        List<RiskMetric> riskMetrics = riskMetricService.getUserRiskMetrics(currentUser.getId());
        return ResponseEntity.ok(riskMetrics);
    }

    /**
     * Calculate stress test for a portfolio
     */
    @PostMapping("/stress-test/{portfolioId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> calculateStressTest(@PathVariable Long portfolioId,
                                                 @RequestParam BigDecimal stressPercentage,
                                                 @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} calculating stress test for portfolio: {} with {}% stress",
                currentUser.getUsername(), portfolioId, stressPercentage.multiply(new BigDecimal("100")));

        try {
            BigDecimal stressedValue = riskMetricService.calculateStressTestValue(portfolioId, stressPercentage);
            return ResponseEntity.ok(new StressTestResponse(portfolioId, stressPercentage, stressedValue));
        } catch (Exception e) {
            log.error("Failed to calculate stress test: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to calculate stress test: " + e.getMessage());
        }
    }

    /**
     * Check if portfolio needs risk recalculation
     */
    @GetMapping("/check-recalculation/{portfolioId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<Boolean> needsRiskRecalculation(@PathVariable Long portfolioId,
                                                          @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} checking if portfolio {} needs risk recalculation",
                currentUser.getUsername(), portfolioId);

        boolean needsRecalculation = riskMetricService.needsRiskRecalculation(portfolioId);
        return ResponseEntity.ok(needsRecalculation);
    }
    /**
     * Manually trigger risk recalculation for all portfolios
     */
    @PostMapping("/recalculate-all")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> recalculateAllRisks(@AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} manually triggering risk recalculation for all portfolios",
                currentUser.getUsername());

        try {
            riskSchedulerService.recalculateAllRisksManually();
            return ResponseEntity.ok("Risk recalculation triggered successfully for all portfolios");
        } catch (Exception e) {
            log.error("Failed to trigger risk recalculation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Failed to trigger risk recalculation: " + e.getMessage());
        }
    }

    // Response DTOs

    public static class StressTestResponse {
        public final Long portfolioId;
        public final BigDecimal stressPercentage;
        public final BigDecimal stressedValue;

        public StressTestResponse(Long portfolioId, BigDecimal stressPercentage, BigDecimal stressedValue) {
            this.portfolioId = portfolioId;
            this.stressPercentage = stressPercentage;
            this.stressedValue = stressedValue;
        }

        // Getters for JSON serialization
        public Long getPortfolioId() {
            return portfolioId;
        }

        public BigDecimal getStressPercentage() {
            return stressPercentage;
        }

        public BigDecimal getStressedValue() {
            return stressedValue;
        }
    }
}