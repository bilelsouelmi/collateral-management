package com.vermeg.collateralmanagement.mapper;

import com.vermeg.collateralmanagement.dto.request.CreateAssetRequest;
import com.vermeg.collateralmanagement.dto.request.UpdateAssetRequest;
import com.vermeg.collateralmanagement.dto.response.AssetResponse;
import com.vermeg.collateralmanagement.entity.CollateralAsset;
import com.vermeg.collateralmanagement.entity.Portfolio;
import com.vermeg.collateralmanagement.enums.AssetStatus;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class AssetMapper {

    private static final Logger log = LoggerFactory.getLogger(AssetMapper.class);

    /**
     * Convert CreateAssetRequest to CollateralAsset entity
     */
    public CollateralAsset toEntity(CreateAssetRequest request) {
        return CollateralAsset.builder()
                .assetId(request.getAssetId())
                .name(request.getName())
                .type(request.getType())
                .marketValue(request.getMarketValue())
                .currency(request.getCurrency())
                .haircut(request.getHaircut())
                .dueDate(request.getDueDate())
                .isEligible(request.getIsEligible() != null ? request.getIsEligible() : true)
                .status(AssetStatus.ACTIVE) // Default status for new assets
                .build();
    }

    /**
     * Update CollateralAsset entity from UpdateAssetRequest
     */
    public void updateEntity(CollateralAsset entity, UpdateAssetRequest request) {
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getType() != null) {
            entity.setType(request.getType());
        }
        if (request.getMarketValue() != null) {
            entity.setMarketValue(request.getMarketValue());
        }
        if (request.getCurrency() != null) {
            entity.setCurrency(request.getCurrency());
        }
        if (request.getHaircut() != null) {
            entity.setHaircut(request.getHaircut());
        }
        if (request.getDueDate() != null) {
            entity.setDueDate(request.getDueDate());
        }
        if (request.getIsEligible() != null) {
            entity.setIsEligible(request.getIsEligible());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        // portfolioId is handled separately in the service layer
    }

    /**
     * Convert CollateralAsset entity to AssetResponse DTO
     */
    public AssetResponse toResponse(CollateralAsset entity) {
        AssetResponse.AssetResponseBuilder builder = AssetResponse.builder()
                .id(entity.getId())
                .assetId(entity.getAssetId())
                .name(entity.getName())
                .type(entity.getType())
                .status(entity.getStatus())
                .marketValue(entity.getMarketValue())
                .currency(entity.getCurrency())
                .haircut(entity.getHaircut() != null ? entity.getHaircut() : BigDecimal.ZERO)
                .dueDate(entity.getDueDate())
                .isEligible(entity.getIsEligible())
                .createdAt(entity.getCreatedAt())
                .lastModifiedAt(entity.getLastModifiedAt());

        // ✅ DEBUG - Portfolio information
        Portfolio portfolio = entity.getPortfolio();

        log.info("=== MAPPER DEBUG ===");
        log.info("Asset ID: {}, Name: {}", entity.getId(), entity.getName());
        log.info("Portfolio object: {}", portfolio);
        log.info("Portfolio is null: {}", portfolio == null);

        if (portfolio != null) {
            log.info("Portfolio ID: {}", portfolio.getId());
            log.info("Portfolio initialized: {}", Hibernate.isInitialized(portfolio));

            try {
                String portfolioName = portfolio.getName();
                log.info("Portfolio Name: '{}'", portfolioName);

                builder.portfolioId(portfolio.getId())
                        .portfolioName(portfolioName);

                log.info("✅ Successfully set portfolioId={}, portfolioName='{}'", portfolio.getId(), portfolioName);
            } catch (Exception e) {
                log.error("❌ ERROR accessing portfolio name: {}", e.getMessage(), e);
                builder.portfolioId(portfolio.getId())
                        .portfolioName(null);
            }
        } else {
            log.warn("⚠️ Portfolio is NULL for asset {}", entity.getId());
        }

        // Calculated fields
        builder.adjustedValue(calculateAdjustedValue(entity))
                .availableForPledging(isAvailableForPledging(entity))
                .nearMaturity(isNearMaturity(entity))
                .daysToMaturity(calculateDaysToMaturity(entity));

        // Valuation information
        builder.lastValuationDate(entity.getLastModifiedAt())
                .previousValue(entity.getMarketValue())
                .valueChange(BigDecimal.ZERO)
                .valueChangePercentage(0.0);

        return builder.build();
    }

    /**
     * Calculate adjusted value (market value minus haircut)
     */
    private BigDecimal calculateAdjustedValue(CollateralAsset entity) {
        if (entity.getMarketValue() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal haircut = entity.getHaircut() != null ? entity.getHaircut() : BigDecimal.ZERO;
        return entity.getMarketValue().multiply(BigDecimal.ONE.subtract(haircut));
    }

    /**
     * Check if asset is available for pledging
     */
    private Boolean isAvailableForPledging(CollateralAsset entity) {
        return entity.getStatus() == AssetStatus.ACTIVE &&
                entity.getIsEligible() &&
                (entity.getDueDate() == null || entity.getDueDate().isAfter(LocalDateTime.now()));
    }

    /**
     * Check if asset is near maturity (within 30 days)
     */
    private Boolean isNearMaturity(CollateralAsset entity) {
        if (entity.getDueDate() == null) {
            return false;
        }
        return entity.getDueDate().isBefore(LocalDateTime.now().plusDays(30));
    }

    /**
     * Calculate days to maturity
     */
    private Integer calculateDaysToMaturity(CollateralAsset entity) {
        if (entity.getDueDate() == null) {
            return null;
        }

        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), entity.getDueDate());
        return Math.toIntExact(days);
    }
}