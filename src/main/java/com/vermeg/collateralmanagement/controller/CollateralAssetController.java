package com.vermeg.collateralmanagement.controller;

import com.vermeg.collateralmanagement.dto.request.CreateAssetRequest;
import com.vermeg.collateralmanagement.dto.request.UpdateAssetRequest;
import com.vermeg.collateralmanagement.dto.response.ApiResponse;
import com.vermeg.collateralmanagement.dto.response.AssetResponse;
import com.vermeg.collateralmanagement.entity.CollateralAsset;
import com.vermeg.collateralmanagement.mapper.AssetMapper;
import com.vermeg.collateralmanagement.service.CollateralAssetService;
import com.vermeg.collateralmanagement.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = "*")
public class CollateralAssetController {

    private static final Logger log = LoggerFactory.getLogger(CollateralAssetController.class);

    @Autowired
    private CollateralAssetService collateralAssetService;

    @Autowired
    private AssetMapper assetMapper;

    /**
     * Add Collateral Asset - Use Case: Add Collateral Asset
     */
    @PostMapping
    @Transactional  // ✅ AJOUTÉ
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<AssetResponse>> addCollateralAsset(
            @Valid @RequestBody CreateAssetRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} adding collateral asset: {}", currentUser.getUsername(), request.getName());

        try {
            CollateralAsset asset = assetMapper.toEntity(request);
            CollateralAsset createdAsset = collateralAssetService.addCollateralAsset(asset, currentUser.getId());
            AssetResponse response = assetMapper.toResponse(createdAsset);

            return ResponseEntity.ok(ApiResponse.success("Asset created successfully", response));
        } catch (Exception e) {
            log.error("Failed to add collateral asset: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to add asset", e.getMessage()));
        }
    }

    /**
     * Get User's Assets
     */
    @GetMapping
    @Transactional(readOnly = true)  // ✅ AJOUTÉ - C'EST LA CLÉ !
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<AssetResponse>>> getUserAssets(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} requesting their assets", currentUser.getUsername());

        List<CollateralAsset> assets = collateralAssetService.getUserAssets(currentUser.getId());
        List<AssetResponse> response = assets.stream()
                .map(assetMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get Asset by ID - Use Case: View Asset Details
     */
    @GetMapping("/{assetId}")
    @Transactional(readOnly = true)  // ✅ AJOUTÉ
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<AssetResponse>> getAssetDetails(
            @PathVariable Long assetId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} requesting asset details for ID: {}", currentUser.getUsername(), assetId);

        try {
            CollateralAsset asset = collateralAssetService.getAssetDetails(assetId, currentUser.getId());
            AssetResponse response = assetMapper.toResponse(asset);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to get asset details: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get asset details", e.getMessage()));
        }
    }

    /**
     * Update Asset
     */
    @PutMapping("/{assetId}")
    @Transactional  // ✅ AJOUTÉ
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<AssetResponse>> updateAsset(
            @PathVariable Long assetId,
            @Valid @RequestBody UpdateAssetRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} updating asset ID: {}", currentUser.getUsername(), assetId);
        log.info("Update request - status: {}", request.getStatus());

        try {
            // Use the NEW DTO-based service method (line 77 in service)
            AssetResponse response = collateralAssetService.updateAsset(assetId, request, currentUser.getId());

            return ResponseEntity.ok(ApiResponse.success("Asset updated successfully", response));
        } catch (Exception e) {
            log.error("Failed to update asset: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update asset", e.getMessage()));
        }
    }

    /**
     * Remove Asset - Use Case: Remove Asset
     */
    @DeleteMapping("/{assetId}")
    @Transactional  // ✅ AJOUTÉ
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<String>> removeAsset(
            @PathVariable Long assetId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} removing asset ID: {}", currentUser.getUsername(), assetId);

        try {
            collateralAssetService.removeAsset(assetId, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Asset removed successfully"));
        } catch (Exception e) {
            log.error("Failed to remove asset: ", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to remove asset", errorMessage));
        }
    }

    /**
     * Assign Asset to Portfolio - Use Case: Assign Asset to Portfolio
     */
    @PostMapping("/{assetId}/assign/{portfolioId}")
    @Transactional  // ✅ AJOUTÉ
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<AssetResponse>> assignAssetToPortfolio(
            @PathVariable Long assetId,
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} assigning asset {} to portfolio {}", currentUser.getUsername(), assetId, portfolioId);

        try {
            CollateralAsset updatedAsset = collateralAssetService.assignAssetToPortfolio(assetId, portfolioId, currentUser.getId());
            AssetResponse response = assetMapper.toResponse(updatedAsset);

            return ResponseEntity.ok(ApiResponse.success("Asset assigned successfully", response));
        } catch (Exception e) {
            log.error("Failed to assign asset to portfolio: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to assign asset", e.getMessage()));
        }
    }

    /**
     * Update Asset Market Value
     */
    @PutMapping("/{assetId}/value")
    @Transactional  // ✅ AJOUTÉ
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<AssetResponse>> updateAssetValue(
            @PathVariable Long assetId,
            @RequestParam BigDecimal newValue,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} updating value for asset ID: {} to {}", currentUser.getUsername(), assetId, newValue);

        try {
            CollateralAsset updatedAsset = collateralAssetService.updateAssetValue(assetId, newValue, currentUser.getId());
            AssetResponse response = assetMapper.toResponse(updatedAsset);

            return ResponseEntity.ok(ApiResponse.success("Asset value updated successfully", response));
        } catch (Exception e) {
            log.error("Failed to update asset value: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update asset value", e.getMessage()));
        }
    }
}