package com.vermeg.collateralmanagement.controller;

import com.vermeg.collateralmanagement.dto.request.CreatePortfolioRequest;
import com.vermeg.collateralmanagement.dto.request.UpdatePortfolioRequest;
import com.vermeg.collateralmanagement.dto.response.ApiResponse;
import com.vermeg.collateralmanagement.dto.response.PortfolioResponse;
import com.vermeg.collateralmanagement.dto.response.PortfolioStatsResponse;
import com.vermeg.collateralmanagement.entity.Portfolio;
import com.vermeg.collateralmanagement.mapper.PortfolioMapper;
import com.vermeg.collateralmanagement.service.PortfolioService;
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
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/portfolios")
@CrossOrigin(origins = "*")
public class PortfolioController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioController.class);

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private PortfolioMapper portfolioMapper;

    @PostMapping
    @Transactional  // ✅ AJOUTÉ
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PortfolioResponse>> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} creating portfolio: {}", currentUser.getUsername(), request.getName());

        try {
            Portfolio portfolio = portfolioMapper.toEntity(request);
            Portfolio createdPortfolio = portfolioService.createPortfolio(portfolio, currentUser.getId());
            PortfolioResponse response = portfolioMapper.toResponse(createdPortfolio);

            return ResponseEntity.ok(ApiResponse.success("Portfolio created successfully", response));
        } catch (Exception e) {
            log.error("Failed to create portfolio: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create portfolio", e.getMessage()));
        }
    }

    @GetMapping
    @Transactional(readOnly = true)  // ✅ AJOUTÉ - IMPORTANT !
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<PortfolioResponse>>> getUserPortfolios(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} requesting their portfolios", currentUser.getUsername());

        List<Portfolio> portfolios = portfolioService.findPortfoliosByUserId(currentUser.getId());
        List<PortfolioResponse> response = portfolios.stream()
                .map(portfolioMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{portfolioId}")
    @Transactional(readOnly = true)  // ✅ AJOUTÉ
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolio(
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} requesting portfolio ID: {}", currentUser.getUsername(), portfolioId);

        try {
            Portfolio portfolio = portfolioService.findPortfolioByIdAndUserId(portfolioId, currentUser.getId());
            PortfolioResponse response = portfolioMapper.toResponse(portfolio);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to get portfolio: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get portfolio", e.getMessage()));
        }
    }

    @PutMapping("/{portfolioId}")
    @Transactional  // ✅ AJOUTÉ
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PortfolioResponse>> updatePortfolio(
            @PathVariable Long portfolioId,
            @Valid @RequestBody UpdatePortfolioRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} updating portfolio ID: {}", currentUser.getUsername(), portfolioId);

        try {
            Portfolio existingPortfolio = portfolioService.findPortfolioByIdAndUserId(portfolioId, currentUser.getId());
            portfolioMapper.updateEntity(existingPortfolio, request);
            Portfolio updatedPortfolio = portfolioService.updatePortfolio(portfolioId, existingPortfolio, currentUser.getId());
            PortfolioResponse response = portfolioMapper.toResponse(updatedPortfolio);

            return ResponseEntity.ok(ApiResponse.success("Portfolio updated successfully", response));
        } catch (Exception e) {
            log.error("Failed to update portfolio: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update portfolio", e.getMessage()));
        }
    }

    @DeleteMapping("/{portfolioId}")
    @Transactional  // ✅ AJOUTÉ
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<String>> deletePortfolio(
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} deleting portfolio ID: {}", currentUser.getUsername(), portfolioId);

        try {
            portfolioService.deletePortfolio(portfolioId, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Portfolio deleted successfully"));
        } catch (Exception e) {
            log.error("Failed to delete portfolio: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to delete portfolio", e.getMessage()));
        }
    }

    @PostMapping("/{portfolioId}/assets/{assetId}")
    @Transactional  // ✅ AJOUTÉ
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<PortfolioResponse>> addAssetToPortfolio(
            @PathVariable Long portfolioId,
            @PathVariable Long assetId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} adding asset {} to portfolio {}", currentUser.getUsername(), assetId, portfolioId);

        try {
            Portfolio updatedPortfolio = portfolioService.addAssetToPortfolio(portfolioId, assetId, currentUser.getId());
            PortfolioResponse response = portfolioMapper.toResponse(updatedPortfolio);

            return ResponseEntity.ok(ApiResponse.success("Asset added successfully", response));
        } catch (Exception e) {
            log.error("Failed to add asset to portfolio: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to add asset", e.getMessage()));
        }
    }

    @DeleteMapping("/{portfolioId}/assets/{assetId}")
    @Transactional  // ✅ AJOUTÉ
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<PortfolioResponse>> removeAssetFromPortfolio(
            @PathVariable Long portfolioId,
            @PathVariable Long assetId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} removing asset {} from portfolio {}", currentUser.getUsername(), assetId, portfolioId);

        try {
            Portfolio updatedPortfolio = portfolioService.removeAssetFromPortfolio(portfolioId, assetId, currentUser.getId());
            PortfolioResponse response = portfolioMapper.toResponse(updatedPortfolio);

            return ResponseEntity.ok(ApiResponse.success("Asset removed successfully", response));
        } catch (Exception e) {
            log.error("Failed to remove asset from portfolio: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to remove asset", e.getMessage()));
        }
    }

    @GetMapping("/{portfolioId}/stats")
    @Transactional(readOnly = true)  // ✅ AJOUTÉ
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PortfolioStatsResponse>> getPortfolioStats(
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} requesting stats for portfolio ID: {}", currentUser.getUsername(), portfolioId);

        try {
            PortfolioService.PortfolioStats stats = portfolioService.getPortfolioStats(portfolioId, currentUser.getId());
            PortfolioStatsResponse response = portfolioMapper.toStatsResponse(stats);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to get portfolio stats: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get portfolio stats", e.getMessage()));
        }
    }

    @PostMapping("/{portfolioId}/recalculate")
    @Transactional  // ✅ AJOUTÉ
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<ApiResponse<PortfolioResponse>> recalculatePortfolioValue(
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} recalculating value for portfolio ID: {}", currentUser.getUsername(), portfolioId);

        try {
            portfolioService.findPortfolioByIdAndUserId(portfolioId, currentUser.getId());
            Portfolio updatedPortfolio = portfolioService.recalculatePortfolioValue(portfolioId);
            PortfolioResponse response = portfolioMapper.toResponse(updatedPortfolio);

            return ResponseEntity.ok(ApiResponse.success("Portfolio value recalculated successfully", response));
        } catch (Exception e) {
            log.error("Failed to recalculate portfolio value: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to recalculate portfolio", e.getMessage()));
        }
    }
}