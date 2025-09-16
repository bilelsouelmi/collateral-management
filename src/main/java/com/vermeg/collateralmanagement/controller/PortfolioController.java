package com.vermeg.collateralmanagement.controller;

import com.vermeg.collateralmanagement.entity.Portfolio;
import com.vermeg.collateralmanagement.service.PortfolioService;
import com.vermeg.collateralmanagement.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/portfolios")
@CrossOrigin(origins = "*")
public class PortfolioController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioController.class);

    @Autowired
    private PortfolioService portfolioService;

    /**
     * Create Portfolio - Use Case: Create Portfolio
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> createPortfolio(@Valid @RequestBody Portfolio portfolio,
                                             @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} creating portfolio: {}", currentUser.getUsername(), portfolio.getName());

        try {
            Portfolio createdPortfolio = portfolioService.createPortfolio(portfolio, currentUser.getId());
            return ResponseEntity.ok(createdPortfolio);
        } catch (Exception e) {
            log.error("Failed to create portfolio: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to create portfolio: " + e.getMessage());
        }
    }

    /**
     * Get User's Portfolios
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<List<Portfolio>> getUserPortfolios(@AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting their portfolios", currentUser.getUsername());

        List<Portfolio> portfolios = portfolioService.findPortfoliosByUserId(currentUser.getId());
        return ResponseEntity.ok(portfolios);
    }

    /**
     * Get Portfolio by ID
     */
    @GetMapping("/{portfolioId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> getPortfolio(@PathVariable Long portfolioId,
                                          @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting portfolio ID: {}", currentUser.getUsername(), portfolioId);

        try {
            Portfolio portfolio = portfolioService.findPortfolioByIdAndUserId(portfolioId, currentUser.getId());
            return ResponseEntity.ok(portfolio);
        } catch (Exception e) {
            log.error("Failed to get portfolio: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update Portfolio
     */
    @PutMapping("/{portfolioId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> updatePortfolio(@PathVariable Long portfolioId,
                                             @Valid @RequestBody Portfolio portfolioUpdate,
                                             @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} updating portfolio ID: {}", currentUser.getUsername(), portfolioId);

        try {
            Portfolio updatedPortfolio = portfolioService.updatePortfolio(portfolioId, portfolioUpdate, currentUser.getId());
            return ResponseEntity.ok(updatedPortfolio);
        } catch (Exception e) {
            log.error("Failed to update portfolio: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to update portfolio: " + e.getMessage());
        }
    }

    /**
     * Delete Portfolio - Use Case: Delete Portfolio
     */
    @DeleteMapping("/{portfolioId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> deletePortfolio(@PathVariable Long portfolioId,
                                             @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} deleting portfolio ID: {}", currentUser.getUsername(), portfolioId);

        try {
            portfolioService.deletePortfolio(portfolioId, currentUser.getId());
            return ResponseEntity.ok("Portfolio deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete portfolio: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to delete portfolio: " + e.getMessage());
        }
    }

    /**
     * Add Asset to Portfolio - Use Case: Manage Portfolio Holdings
     */
    @PostMapping("/{portfolioId}/assets/{assetId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> addAssetToPortfolio(@PathVariable Long portfolioId,
                                                 @PathVariable Long assetId,
                                                 @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} adding asset {} to portfolio {}", currentUser.getUsername(), assetId, portfolioId);

        try {
            Portfolio updatedPortfolio = portfolioService.addAssetToPortfolio(portfolioId, assetId, currentUser.getId());
            return ResponseEntity.ok(updatedPortfolio);
        } catch (Exception e) {
            log.error("Failed to add asset to portfolio: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to add asset: " + e.getMessage());
        }
    }

    /**
     * Remove Asset from Portfolio - Use Case: Manage Portfolio Holdings
     */
    @DeleteMapping("/{portfolioId}/assets/{assetId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> removeAssetFromPortfolio(@PathVariable Long portfolioId,
                                                      @PathVariable Long assetId,
                                                      @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} removing asset {} from portfolio {}", currentUser.getUsername(), assetId, portfolioId);

        try {
            Portfolio updatedPortfolio = portfolioService.removeAssetFromPortfolio(portfolioId, assetId, currentUser.getId());
            return ResponseEntity.ok(updatedPortfolio);
        } catch (Exception e) {
            log.error("Failed to remove asset from portfolio: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to remove asset: " + e.getMessage());
        }
    }

    /**
     * Get Portfolio Statistics
     */
    @GetMapping("/{portfolioId}/stats")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER') or hasRole('MANAGER')")
    public ResponseEntity<?> getPortfolioStats(@PathVariable Long portfolioId,
                                               @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} requesting stats for portfolio ID: {}", currentUser.getUsername(), portfolioId);

        try {
            PortfolioService.PortfolioStats stats = portfolioService.getPortfolioStats(portfolioId, currentUser.getId());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get portfolio stats: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to get portfolio stats: " + e.getMessage());
        }
    }

    /**
     * Recalculate Portfolio Value
     */
    @PostMapping("/{portfolioId}/recalculate")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('RISK_OFFICER')")
    public ResponseEntity<?> recalculatePortfolioValue(@PathVariable Long portfolioId,
                                                       @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} recalculating value for portfolio ID: {}", currentUser.getUsername(), portfolioId);

        try {
            // Verify user owns the portfolio
            portfolioService.findPortfolioByIdAndUserId(portfolioId, currentUser.getId());
            Portfolio updatedPortfolio = portfolioService.recalculatePortfolioValue(portfolioId);
            return ResponseEntity.ok(updatedPortfolio);
        } catch (Exception e) {
            log.error("Failed to recalculate portfolio value: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to recalculate portfolio: " + e.getMessage());
        }
    }
}