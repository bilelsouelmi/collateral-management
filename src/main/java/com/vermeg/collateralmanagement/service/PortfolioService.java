package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.entity.Portfolio;
import com.vermeg.collateralmanagement.entity.CollateralAsset;
import com.vermeg.collateralmanagement.entity.User;
import com.vermeg.collateralmanagement.repository.PortfolioRepository;
import com.vermeg.collateralmanagement.repository.CollateralAssetRepository;
import com.vermeg.collateralmanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private CollateralAssetRepository collateralAssetRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create Portfolio - Use Case Implementation
     */
    public Portfolio createPortfolio(Portfolio portfolio, Long userId) {
        log.info("Creating portfolio: {} for user ID: {}", portfolio.getName(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        portfolio.setUser(user);
        portfolio.setTotalValue(BigDecimal.ZERO);
        portfolio.setTotalMargin(BigDecimal.ZERO);

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        log.info("Portfolio created successfully with ID: {}", savedPortfolio.getId());

        return savedPortfolio;
    }

    /**
     * Delete Portfolio - Use Case Implementation
     */
    public void deletePortfolio(Long portfolioId, Long userId) {
        log.info("Deleting portfolio ID: {} for user ID: {}", portfolioId, userId);

        Portfolio portfolio = findPortfolioByIdAndUserId(portfolioId, userId);

        // Check if portfolio has assets
        List<CollateralAsset> assets = collateralAssetRepository.findByPortfolioId(portfolioId);
        if (!assets.isEmpty()) {
            throw new RuntimeException("Cannot delete portfolio with existing assets. Remove all assets first.");
        }

        portfolioRepository.delete(portfolio);
        log.info("Portfolio deleted successfully: {}", portfolioId);
    }

    /**
     * Manage Portfolio Holdings - Use Case Implementation
     */
    public Portfolio addAssetToPortfolio(Long portfolioId, Long assetId, Long userId) {
        log.info("Adding asset ID: {} to portfolio ID: {} for user ID: {}", assetId, portfolioId, userId);

        Portfolio portfolio = findPortfolioByIdAndUserId(portfolioId, userId);
        CollateralAsset asset = collateralAssetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found with ID: " + assetId));

        // Check if asset is already in a portfolio
        if (asset.getPortfolio() != null) {
            throw new RuntimeException("Asset is already assigned to portfolio: " + asset.getPortfolio().getName());
        }

        asset.setPortfolio(portfolio);
        collateralAssetRepository.save(asset);

        // Recalculate portfolio value
        Portfolio updatedPortfolio = recalculatePortfolioValue(portfolioId);
        log.info("Asset added to portfolio successfully");

        return updatedPortfolio;
    }

    public Portfolio removeAssetFromPortfolio(Long portfolioId, Long assetId, Long userId) {
        log.info("Removing asset ID: {} from portfolio ID: {} for user ID: {}", assetId, portfolioId, userId);

        Portfolio portfolio = findPortfolioByIdAndUserId(portfolioId, userId);
        CollateralAsset asset = collateralAssetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found with ID: " + assetId));

        // Verify asset belongs to this portfolio
        if (asset.getPortfolio() == null || !asset.getPortfolio().getId().equals(portfolioId)) {
            throw new RuntimeException("Asset does not belong to this portfolio");
        }

        asset.setPortfolio(null);
        collateralAssetRepository.save(asset);

        // Recalculate portfolio value
        Portfolio updatedPortfolio = recalculatePortfolioValue(portfolioId);
        log.info("Asset removed from portfolio successfully");

        return updatedPortfolio;
    }

    /**
     * Find user's portfolios
     */
    public List<Portfolio> findPortfoliosByUserId(Long userId) {
        log.debug("Finding portfolios for user ID: {}", userId);
        return portfolioRepository.findByUserId(userId);
    }

    /**
     * Find portfolio by ID and user ID (security check)
     */
    public Portfolio findPortfolioByIdAndUserId(Long portfolioId, Long userId) {
        return portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found or access denied"));
    }

    /**
     * Get portfolio details
     */
    public Portfolio getPortfolioById(Long portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found with ID: " + portfolioId));
    }

    /**
     * Update portfolio information
     */
    public Portfolio updatePortfolio(Long portfolioId, Portfolio portfolioUpdate, Long userId) {
        log.info("Updating portfolio ID: {} for user ID: {}", portfolioId, userId);

        Portfolio existingPortfolio = findPortfolioByIdAndUserId(portfolioId, userId);

        // Update allowed fields
        existingPortfolio.setName(portfolioUpdate.getName());
        existingPortfolio.setDescription(portfolioUpdate.getDescription());
        existingPortfolio.setType(portfolioUpdate.getType());

        Portfolio updatedPortfolio = portfolioRepository.save(existingPortfolio);
        log.info("Portfolio updated successfully: {}", portfolioId);

        return updatedPortfolio;
    }

    /**
     * Recalculate portfolio total value
     */
    public Portfolio recalculatePortfolioValue(Long portfolioId) {
        log.debug("Recalculating portfolio value for ID: {}", portfolioId);

        Portfolio portfolio = getPortfolioById(portfolioId);
        List<CollateralAsset> assets = collateralAssetRepository.findByPortfolioId(portfolioId);

        BigDecimal totalValue = assets.stream()
                .map(CollateralAsset::getMarketValue)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        portfolio.setTotalValue(totalValue);

        return portfolioRepository.save(portfolio);
    }

    /**
     * Get portfolio statistics
     */
    public PortfolioStats getPortfolioStats(Long portfolioId, Long userId) {
        Portfolio portfolio = findPortfolioByIdAndUserId(portfolioId, userId);
        List<CollateralAsset> assets = collateralAssetRepository.findByPortfolioId(portfolioId);

        long activeAssets = assets.stream()
                .filter(asset -> asset.getIsEligible() != null && asset.getIsEligible())
                .count();

        return new PortfolioStats(
                portfolioId,
                assets.size(),
                portfolio.getTotalValue(),
                portfolio.getTotalMargin(),
                (int) activeAssets
        );
    }

    // Inner class for portfolio statistics
    public static class PortfolioStats {
        private Long portfolioId;
        private int totalAssets;
        private BigDecimal totalValue;
        private BigDecimal totalMargin;
        private int activeAssets;

        public PortfolioStats(Long portfolioId, int totalAssets, BigDecimal totalValue,
                              BigDecimal totalMargin, int activeAssets) {
            this.portfolioId = portfolioId;
            this.totalAssets = totalAssets;
            this.totalValue = totalValue;
            this.totalMargin = totalMargin;
            this.activeAssets = activeAssets;
        }

        // Getters
        public Long getPortfolioId() { return portfolioId; }
        public int getTotalAssets() { return totalAssets; }
        public BigDecimal getTotalValue() { return totalValue; }
        public BigDecimal getTotalMargin() { return totalMargin; }
        public int getActiveAssets() { return activeAssets; }
    }
}