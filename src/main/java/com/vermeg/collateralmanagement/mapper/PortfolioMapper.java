package com.vermeg.collateralmanagement.mapper;

import com.vermeg.collateralmanagement.dto.request.CreatePortfolioRequest;
import com.vermeg.collateralmanagement.dto.request.UpdatePortfolioRequest;
import com.vermeg.collateralmanagement.dto.response.PortfolioResponse;
import com.vermeg.collateralmanagement.dto.response.PortfolioStatsResponse;
import com.vermeg.collateralmanagement.entity.CollateralAsset;
import com.vermeg.collateralmanagement.entity.Portfolio;
import com.vermeg.collateralmanagement.service.PortfolioService;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class PortfolioMapper {

    private static final Logger log = LoggerFactory.getLogger(PortfolioMapper.class);

    /**
     * Convert CreatePortfolioRequest to Portfolio entity
     */
    public Portfolio toEntity(CreatePortfolioRequest request) {
        return Portfolio.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .totalValue(BigDecimal.ZERO)
                .totalMargin(BigDecimal.ZERO)
                .build();
    }

    /**
     * Update Portfolio entity from UpdatePortfolioRequest
     */
    public void updateEntity(Portfolio entity, UpdatePortfolioRequest request) {
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getType() != null) {
            entity.setType(request.getType());
        }
    }

    /**
     * Convert Portfolio entity to PortfolioResponse DTO
     */
    public PortfolioResponse toResponse(Portfolio entity) {
        PortfolioResponse.PortfolioResponseBuilder builder = PortfolioResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .type(entity.getType())
                .totalValue(entity.getTotalValue())
                .totalMargin(entity.getTotalMargin())
                .createdAt(entity.getCreatedAt())
                .lastModifiedAt(entity.getLastModifiedAt());

        // User information
        if (entity.getUser() != null) {
            try {
                if (Hibernate.isInitialized(entity.getUser())) {
                    builder.userId(entity.getUser().getId())
                            .username(entity.getUser().getUsername());
                } else {
                    builder.userId(entity.getUser().getId())
                            .username(null);
                }
            } catch (Exception e) {
                log.warn("Could not load user for portfolio {}: {}", entity.getId(), e.getMessage());
            }
        }

        // ✅ CORRECTION - Asset statistics avec protection contre ConcurrentModificationException
        try {
            List<CollateralAsset> assets = entity.getAssets();

            if (assets != null && Hibernate.isInitialized(assets)) {
                // Copier dans une liste pour éviter ConcurrentModificationException
                List<CollateralAsset> assetsList = new ArrayList<>(assets);

                builder.assetCount(assetsList.size());

                long activeCount = assetsList.stream()
                        .filter(asset -> asset.getIsEligible() != null && asset.getIsEligible())
                        .count();

                builder.activeAssetCount((int) activeCount);
            } else {
                // Assets non initialisés
                builder.assetCount(0)
                        .activeAssetCount(0);
            }
        } catch (Exception e) {
            log.warn("Could not load assets for portfolio {}: {}", entity.getId(), e.getMessage());
            builder.assetCount(0)
                    .activeAssetCount(0);
        }

        // Performance metrics (placeholder - you can enhance this)
        builder.dailyChange(BigDecimal.ZERO)
                .dailyChangePercentage(0.0);

        return builder.build();
    }

    /**
     * Convert PortfolioStats to PortfolioStatsResponse
     */
    public PortfolioStatsResponse toStatsResponse(PortfolioService.PortfolioStats stats) {
        return PortfolioStatsResponse.builder()
                .portfolioId(stats.getPortfolioId())
                .totalAssets(stats.getTotalAssets())
                .totalValue(stats.getTotalValue())
                .totalMargin(stats.getTotalMargin())
                .activeAssets(stats.getActiveAssets())
                .build();
    }
}