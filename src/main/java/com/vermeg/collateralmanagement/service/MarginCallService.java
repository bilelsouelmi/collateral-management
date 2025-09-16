package com.vermeg.collateralmanagement.service;

import com.vermeg.collateralmanagement.entity.*;
import com.vermeg.collateralmanagement.enums.MarginCallStatus;
import com.vermeg.collateralmanagement.enums.AssetStatus;
import com.vermeg.collateralmanagement.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MarginCallService {

    private static final Logger log = LoggerFactory.getLogger(MarginCallService.class);

    private static final BigDecimal DEFAULT_MARGIN_REQUIREMENT = new BigDecimal("0.20"); // 20%
    private static final BigDecimal CONCENTRATION_LIMIT = new BigDecimal("0.30"); // 30%
    private static final int MARGIN_CALL_DUE_DAYS = 3;

    @Autowired
    private MarginCallRepository marginCallRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private CollateralAssetRepository collateralAssetRepository;

    @Autowired
    private AlertService alertService;

    @Autowired
    private RiskMetricService riskMetricService;

    /**
     * Calculate margin requirements for a portfolio
     */
    public BigDecimal calculatePortfolioMarginRequirement(Long portfolioId) {
        log.info("Calculating margin requirement for portfolio: {}", portfolioId);

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found: " + portfolioId));

        List<CollateralAsset> eligibleAssets = collateralAssetRepository
                .findByPortfolioIdAndStatus(portfolioId, AssetStatus.ACTIVE)
                .stream()
                .filter(CollateralAsset::getIsEligible)
                .toList();

        BigDecimal totalEligibleValue = eligibleAssets.stream()
                .map(CollateralAsset::getAdjustedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal baseMarginRequirement = totalEligibleValue.multiply(DEFAULT_MARGIN_REQUIREMENT);
        BigDecimal concentrationAdjustment = calculateConcentrationRiskAdjustment(eligibleAssets, totalEligibleValue);

        return baseMarginRequirement.add(concentrationAdjustment);
    }

    /**
     * Monitor exposure limits for a user
     */
    public void monitorExposureLimits(Long userId) {
        log.info("Monitoring exposure limits for user: {}", userId);

        List<Portfolio> userPortfolios = portfolioRepository.findByUserId(userId);

        for (Portfolio portfolio : userPortfolios) {
            BigDecimal requiredMargin = calculatePortfolioMarginRequirement(portfolio.getId());
            BigDecimal currentMargin = portfolio.getTotalMargin();

            if (currentMargin.compareTo(requiredMargin) < 0) {
                BigDecimal shortfall = requiredMargin.subtract(currentMargin);
                createMarginCall(portfolio, requiredMargin, currentMargin, shortfall);
                alertService.createMarginAlert(portfolio.getUser(), portfolio, shortfall);
            }

            checkConcentrationLimits(portfolio);
        }
    }

    /**
     * Perform comprehensive risk assessment
     */
    public void performRiskAssessment(Long portfolioId) {
        log.info("Performing risk assessment for portfolio: {}", portfolioId);

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found: " + portfolioId));

        BigDecimal portfolioValue = portfolio.getTotalValue();
        BigDecimal marginRequirement = calculatePortfolioMarginRequirement(portfolioId);
        BigDecimal concentrationRisk = calculateConcentrationRisk(portfolioId);
        BigDecimal liquidityRisk = calculateLiquidityRisk(portfolioId);

        RiskMetric riskMetric = riskMetricService.createRiskMetric(
                portfolio, portfolioValue, marginRequirement, concentrationRisk, liquidityRisk);

        if (riskMetric.getValue().compareTo(new BigDecimal("0.75")) > 0) {
            alertService.createRiskAlert(portfolio.getUser(), portfolio, riskMetric);
        }
    }

    /**
     * Create or update margin call
     */
    public MarginCall createMarginCall(Portfolio portfolio, BigDecimal requiredMargin,
                                       BigDecimal currentMargin, BigDecimal shortfall) {
        log.info("Creating margin call for portfolio: {} with shortfall: {}",
                portfolio.getId(), shortfall);

        Optional<MarginCall> existingCall = marginCallRepository
                .findActiveMarginCallByPortfolio(portfolio.getId());

        if (existingCall.isPresent()) {
            MarginCall marginCall = existingCall.get();
            marginCall.setRequiredMargin(requiredMargin);
            marginCall.setCurrentMargin(currentMargin);
            marginCall.setShortfall(shortfall);
            marginCall.setLastModifiedAt(LocalDateTime.now());
            return marginCallRepository.save(marginCall);
        } else {
            MarginCall marginCall = MarginCall.builder()
                    .portfolio(portfolio)
                    .requiredMargin(requiredMargin)
                    .currentMargin(currentMargin)
                    .shortfall(shortfall)
                    .dueDate(LocalDateTime.now().plusDays(MARGIN_CALL_DUE_DAYS))
                    .status(MarginCallStatus.PENDING)
                    .build();

            return marginCallRepository.save(marginCall);
        }
    }

    /**
     * Get active margin calls by user
     */
    public List<MarginCall> getActiveMarginCallsByUser(Long userId) {
        return marginCallRepository.findActiveMarginCallsByUserId(userId);
    }

    /**
     * Get margin call details with access control
     */
    public MarginCall getMarginCallDetails(Long marginCallId, Long userId) {
        MarginCall marginCall = marginCallRepository.findById(marginCallId)
                .orElseThrow(() -> new RuntimeException("Margin call not found: " + marginCallId));

        if (!marginCall.getPortfolio().getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied: Margin call belongs to another user");
        }

        return marginCall;
    }

    /**
     * Acknowledge margin call
     */
    public MarginCall acknowledgeMarginCall(Long marginCallId, Long userId) {
        log.info("User {} acknowledging margin call: {}", userId, marginCallId);

        MarginCall marginCall = getMarginCallDetails(marginCallId, userId);
        marginCall.setStatus(MarginCallStatus.ACKNOWLEDGED);
        marginCall.setLastModifiedAt(LocalDateTime.now());

        return marginCallRepository.save(marginCall);
    }

    /**
     * Resolve margin call
     */
    public MarginCall resolveMarginCall(Long marginCallId, Long userId) {
        log.info("User {} resolving margin call: {}", userId, marginCallId);

        MarginCall marginCall = getMarginCallDetails(marginCallId, userId);

        BigDecimal currentMargin = calculatePortfolioMarginRequirement(marginCall.getPortfolio().getId());

        if (currentMargin.compareTo(marginCall.getRequiredMargin()) >= 0) {
            marginCall.setStatus(MarginCallStatus.RESOLVED);
            marginCall.setCurrentMargin(currentMargin);
            marginCall.setShortfall(BigDecimal.ZERO);
            marginCall.setLastModifiedAt(LocalDateTime.now());
        } else {
            throw new RuntimeException("Margin call cannot be resolved: insufficient margin coverage");
        }

        return marginCallRepository.save(marginCall);
    }

    /**
     * Get overdue margin calls
     */
    public List<MarginCall> getOverdueMarginCalls() {
        return marginCallRepository.findOverdueMarginCalls(LocalDateTime.now());
    }

    // Private helper methods

    private BigDecimal calculateConcentrationRiskAdjustment(List<CollateralAsset> assets, BigDecimal totalValue) {
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal concentrationPenalty = BigDecimal.ZERO;

        for (CollateralAsset asset : assets) {
            BigDecimal assetWeight = asset.getAdjustedValue().divide(totalValue, 4, RoundingMode.HALF_UP);

            if (assetWeight.compareTo(CONCENTRATION_LIMIT) > 0) {
                BigDecimal excessWeight = assetWeight.subtract(CONCENTRATION_LIMIT);
                concentrationPenalty = concentrationPenalty.add(
                        asset.getAdjustedValue().multiply(excessWeight).multiply(new BigDecimal("0.1"))
                );
            }
        }

        return concentrationPenalty;
    }

    private void checkConcentrationLimits(Portfolio portfolio) {
        List<CollateralAsset> assets = collateralAssetRepository.findByPortfolioId(portfolio.getId());
        BigDecimal totalValue = portfolio.getTotalValue();

        for (CollateralAsset asset : assets) {
            if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal concentration = asset.getMarketValue().divide(totalValue, 4, RoundingMode.HALF_UP);

                if (concentration.compareTo(CONCENTRATION_LIMIT) > 0) {
                    alertService.createConcentrationAlert(portfolio.getUser(), portfolio, asset, concentration);
                }
            }
        }
    }

    private BigDecimal calculateConcentrationRisk(Long portfolioId) {
        List<CollateralAsset> assets = collateralAssetRepository.findByPortfolioId(portfolioId);
        BigDecimal totalValue = assets.stream()
                .map(CollateralAsset::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return assets.stream()
                .map(asset -> {
                    BigDecimal weight = asset.getMarketValue().divide(totalValue, 4, RoundingMode.HALF_UP);
                    return weight.multiply(weight);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateLiquidityRisk(Long portfolioId) {
        List<CollateralAsset> assets = collateralAssetRepository.findByPortfolioId(portfolioId);

        BigDecimal liquidityScore = assets.stream()
                .map(asset -> {
                    switch (asset.getType()) {
                        case CASH: return new BigDecimal("0.0");
                        case GOVERNMENT_BOND: return new BigDecimal("0.1");
                        case CORPORATE_BOND: return new BigDecimal("0.2");
                        case EQUITY: return new BigDecimal("0.3");
                        case COMMODITY: return new BigDecimal("0.4");
                        case REAL_ESTATE: return new BigDecimal("0.5");
                        default: return new BigDecimal("0.3");
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return assets.isEmpty() ? BigDecimal.ZERO :
                liquidityScore.divide(new BigDecimal(assets.size()), 4, RoundingMode.HALF_UP);
    }
}