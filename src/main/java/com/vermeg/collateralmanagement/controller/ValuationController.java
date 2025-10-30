package com.vermeg.collateralmanagement.controller;

import com.vermeg.collateralmanagement.entity.CollateralAsset;
import com.vermeg.collateralmanagement.entity.Valuation;
import com.vermeg.collateralmanagement.service.ValuationService;
import com.vermeg.collateralmanagement.security.UserPrincipal;
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
import java.util.Map;

@RestController
@RequestMapping("/api/valuations")
@CrossOrigin(origins = "*")
public class ValuationController {

    private static final Logger log = LoggerFactory.getLogger(ValuationController.class);

    @Autowired
    private ValuationService valuationService;

    /**
     * Update asset market value - Use Case: Price Feed Interface
     */
    @PutMapping("/assets/{assetId}/value")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> updateAssetValue(@PathVariable Long assetId,
                                              @RequestParam BigDecimal newValue,
                                              @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} updating value for asset {} to {}",
                currentUser.getUsername(), assetId, newValue);

        try {
            CollateralAsset updatedAsset = valuationService.updateAssetMarketValue(
                    assetId, newValue, currentUser.getId());
            return ResponseEntity.ok(updatedAsset);
        } catch (Exception e) {
            log.error("Failed to update asset value: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to update asset value: " + e.getMessage());
        }
    }

    /**
     * Get latest valuation for an asset
     */
    @GetMapping("/assets/{assetId}/latest")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> getLatestValuation(@PathVariable Long assetId,
                                                @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting latest valuation for asset: {}",
                currentUser.getUsername(), assetId);

        try {
            Valuation valuation = valuationService.getLatestValuation(assetId);
            if (valuation != null) {
                return ResponseEntity.ok(valuation);
            } else {
                return ResponseEntity.ok("No valuations found for asset");
            }
        } catch (Exception e) {
            log.error("Failed to get latest valuation: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to get valuation: " + e.getMessage());
        }
    }

    /**
     * Get valuation history for an asset
     */
    @GetMapping("/assets/{assetId}/history")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> getValuationHistory(@PathVariable Long assetId,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                                 @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting valuation history for asset: {} from {} to {}",
                currentUser.getUsername(), assetId, startDate, endDate);

        try {
            List<Valuation> valuations = valuationService.getValuationHistory(assetId, startDate, endDate);
            return ResponseEntity.ok(valuations);
        } catch (Exception e) {
            log.error("Failed to get valuation history: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to get valuation history: " + e.getMessage());
        }
    }

    /**
     * Get portfolio current value
     */
    @GetMapping("/portfolios/{portfolioId}/current-value")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> getPortfolioCurrentValue(@PathVariable Long portfolioId,
                                                      @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting current value for portfolio: {}",
                currentUser.getUsername(), portfolioId);

        try {
            BigDecimal currentValue = valuationService.getPortfolioCurrentValue(portfolioId);
            return ResponseEntity.ok(new PortfolioValueResponse(portfolioId, currentValue));
        } catch (Exception e) {
            log.error("Failed to get portfolio current value: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to get portfolio value: " + e.getMessage());
        }
    }

    /**
     * Bulk update asset values - Use Case: Market Data Integration
     */
    @PostMapping("/bulk-update")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> bulkUpdateAssetValues(@RequestBody Map<String, BigDecimal> assetUpdates,
                                                   @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} performing bulk valuation update for {} assets",
                currentUser.getUsername(), assetUpdates.size());

        try {
            valuationService.bulkUpdateAssetValues(assetUpdates);
            return ResponseEntity.ok("Bulk valuation update completed successfully");
        } catch (Exception e) {
            log.error("Failed to perform bulk update: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to perform bulk update: " + e.getMessage());
        }
    }

    /**
     * Simulate market data update - Use Case: Market Data Integration
     */
    @PostMapping("/simulate-market-update")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> simulateMarketDataUpdate(@AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} initiating market data simulation", currentUser.getUsername());

        try {
            valuationService.simulateMarketDataUpdate();
            return ResponseEntity.ok("Market data simulation completed successfully");
        } catch (Exception e) {
            log.error("Failed to simulate market data: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to simulate market data: " + e.getMessage());
        }
    }

    /**
     * Find stale valuations
     */
    @GetMapping("/stale")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<List<CollateralAsset>> findStaleValuations(@RequestParam(defaultValue = "24") int hoursThreshold,
                                                                     @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting stale valuations older than {} hours",
                currentUser.getUsername(), hoursThreshold);

        List<CollateralAsset> staleAssets = valuationService.findStaleValuations(hoursThreshold);
        return ResponseEntity.ok(staleAssets);
    }

    /**
     * Get asset value at specific date
     */
    @GetMapping("/assets/{assetId}/value-at-date")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> getAssetValueAtDate(@PathVariable Long assetId,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime targetDate,
                                                 @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting asset {} value at date: {}",
                currentUser.getUsername(), assetId, targetDate);

        try {
            BigDecimal value = valuationService.getAssetValueAtDate(assetId, targetDate);
            return ResponseEntity.ok(new AssetValueAtDateResponse(assetId, targetDate, value));
        } catch (Exception e) {
            log.error("Failed to get asset value at date: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to get asset value: " + e.getMessage());
        }
    }

    /**
     * Calculate asset volatility
     */
    @GetMapping("/assets/{assetId}/volatility")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> calculateAssetVolatility(@PathVariable Long assetId,
                                                      @RequestParam(defaultValue = "30") int days,
                                                      @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} calculating volatility for asset {} over {} days",
                currentUser.getUsername(), assetId, days);

        try {
            BigDecimal volatility = valuationService.calculateAssetVolatility(assetId, days);
            return ResponseEntity.ok(new AssetVolatilityResponse(assetId, days, volatility));
        } catch (Exception e) {
            log.error("Failed to calculate asset volatility: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to calculate volatility: " + e.getMessage());
        }
    }

    /**
     * Get valuation statistics for an asset
     */
    @GetMapping("/assets/{assetId}/statistics")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> getValuationStatistics(@PathVariable Long assetId,
                                                    @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting valuation statistics for asset: {}",
                currentUser.getUsername(), assetId);

        try {
            ValuationService.ValuationStatistics stats = valuationService.getValuationStatistics(assetId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get valuation statistics: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to get statistics: " + e.getMessage());
        }
    }

    // Response DTOs
    public static class PortfolioValueResponse {
        public final Long portfolioId;
        public final BigDecimal currentValue;

        public PortfolioValueResponse(Long portfolioId, BigDecimal currentValue) {
            this.portfolioId = portfolioId;
            this.currentValue = currentValue;
        }
    }

    public static class AssetValueAtDateResponse {
        public final Long assetId;
        public final LocalDateTime date;
        public final BigDecimal value;

        public AssetValueAtDateResponse(Long assetId, LocalDateTime date, BigDecimal value) {
            this.assetId = assetId;
            this.date = date;
            this.value = value;
        }
    }

    public static class AssetVolatilityResponse {
        public final Long assetId;
        public final Integer days;
        public final BigDecimal volatility;

        public AssetVolatilityResponse(Long assetId, Integer days, BigDecimal volatility) {
            this.assetId = assetId;
            this.days = days;
            this.volatility = volatility;
        }
    }
}