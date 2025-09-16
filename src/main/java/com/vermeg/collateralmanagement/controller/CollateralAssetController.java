package com.vermeg.collateralmanagement.controller;

import com.vermeg.collateralmanagement.entity.CollateralAsset;
import com.vermeg.collateralmanagement.service.CollateralAssetService;
import com.vermeg.collateralmanagement.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/assets")
@CrossOrigin(origins = "*")
public class CollateralAssetController {

    private static final Logger log = LoggerFactory.getLogger(CollateralAssetController.class);

    @Autowired
    private CollateralAssetService collateralAssetService;

    /**
     * Add Collateral Asset - Use Case: Add Collateral Asset
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> addCollateralAsset(@Valid @RequestBody CollateralAsset asset,
                                                @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} adding collateral asset: {}", currentUser.getUsername(), asset.getName());

        try {
            CollateralAsset createdAsset = collateralAssetService.addCollateralAsset(asset, currentUser.getId());
            return ResponseEntity.ok(createdAsset);
        } catch (Exception e) {
            log.error("Failed to add collateral asset: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to add asset: " + e.getMessage());
        }
    }

    /**
     * Get User's Assets
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<List<CollateralAsset>> getUserAssets(@AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting their assets", currentUser.getUsername());

        List<CollateralAsset> assets = collateralAssetService.getUserAssets(currentUser.getId());
        return ResponseEntity.ok(assets);
    }

    /**
     * Get Asset by ID - Use Case: View Asset Details
     */
    @GetMapping("/{assetId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> getAssetDetails(@PathVariable Long assetId,
                                             @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting asset details for ID: {}", currentUser.getUsername(), assetId);

        try {
            CollateralAsset asset = collateralAssetService.getAssetDetails(assetId, currentUser.getId());
            return ResponseEntity.ok(asset);
        } catch (Exception e) {
            log.error("Failed to get asset details: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update Asset
     */
    @PutMapping("/{assetId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> updateAsset(@PathVariable Long assetId,
                                         @Valid @RequestBody CollateralAsset assetUpdate,
                                         @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} updating asset ID: {}", currentUser.getUsername(), assetId);

        try {
            CollateralAsset updatedAsset = collateralAssetService.updateAsset(assetId, assetUpdate, currentUser.getId());
            return ResponseEntity.ok(updatedAsset);
        } catch (Exception e) {
            log.error("Failed to update asset: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to update asset: " + e.getMessage());
        }
    }

    /**
     * Remove Asset - Use Case: Remove Asset
     */
    @DeleteMapping("/{assetId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> removeAsset(@PathVariable Long assetId,
                                         @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} removing asset ID: {}", currentUser.getUsername(), assetId);

        try {
            collateralAssetService.removeAsset(assetId, currentUser.getId());
            return ResponseEntity.ok("Asset removed successfully");
        } catch (Exception e) {
            log.error("Failed to remove asset: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to remove asset: " + e.getMessage());
        }
    }

    /**
     * Assign Asset to Portfolio - Use Case: Assign Asset to Portfolio
     */
    @PostMapping("/{assetId}/assign/{portfolioId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> assignAssetToPortfolio(@PathVariable Long assetId,
                                                    @PathVariable Long portfolioId,
                                                    @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} assigning asset {} to portfolio {}", currentUser.getUsername(), assetId, portfolioId);

        try {
            CollateralAsset updatedAsset = collateralAssetService.assignAssetToPortfolio(assetId, portfolioId, currentUser.getId());
            return ResponseEntity.ok(updatedAsset);
        } catch (Exception e) {
            log.error("Failed to assign asset to portfolio: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to assign asset: " + e.getMessage());
        }
    }

    /**
     * Update Asset Market Value
     */
    @PutMapping("/{assetId}/value")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> updateAssetValue(@PathVariable Long assetId,
                                              @RequestParam BigDecimal newValue,
                                              @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} updating value for asset ID: {} to {}", currentUser.getUsername(), assetId, newValue);

        try {
            CollateralAsset updatedAsset = collateralAssetService.updateAssetValue(assetId, newValue, currentUser.getId());
            return ResponseEntity.ok(updatedAsset);
        } catch (Exception e) {
            log.error("Failed to update asset value: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to update asset value: " + e.getMessage());
        }
    }

    /**
     * Get Unassigned Assets
     */
    @GetMapping("/unassigned")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<List<CollateralAsset>> getUnassignedAssets(@AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting unassigned assets", currentUser.getUsername());

        List<CollateralAsset> unassignedAssets = collateralAssetService.getUnassignedAssets();
        return ResponseEntity.ok(unassignedAssets);
    }

    /**
     * Get Assets by Portfolio
     */
    @GetMapping("/portfolio/{portfolioId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> getAssetsByPortfolio(@PathVariable Long portfolioId,
                                                  @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting assets for portfolio ID: {}", currentUser.getUsername(), portfolioId);

        try {
            List<CollateralAsset> assets = collateralAssetService.getAssetsByPortfolio(portfolioId, currentUser.getId());
            return ResponseEntity.ok(assets);
        } catch (Exception e) {
            log.error("Failed to get portfolio assets: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to get portfolio assets: " + e.getMessage());
        }
    }

    /**
     * Get Assets Nearing Maturity
     */
    @GetMapping("/maturity/{daysThreshold}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<List<CollateralAsset>> getAssetsNearingMaturity(@PathVariable int daysThreshold,
                                                                          @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting assets nearing maturity within {} days", currentUser.getUsername(), daysThreshold);

        List<CollateralAsset> nearingMaturity = collateralAssetService.getAssetsNearingMaturity(daysThreshold, currentUser.getId());
        return ResponseEntity.ok(nearingMaturity);
    }

    /**
     * Find Asset by Asset ID (business identifier)
     */
    @GetMapping("/search/{assetId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> findAssetByAssetId(@PathVariable String assetId,
                                                @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} searching for asset with ID: {}", currentUser.getUsername(), assetId);

        try {
            CollateralAsset asset = collateralAssetService.findAssetByAssetId(assetId);
            return ResponseEntity.ok(asset);
        } catch (Exception e) {
            log.error("Asset not found with ID: {}", assetId);
            return ResponseEntity.notFound().build();
        }
    }
}