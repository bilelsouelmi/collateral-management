// src/main/java/com/vermeg/collateralmanagement/dto/response/PortfolioResponse.java
package com.vermeg.collateralmanagement.dto.response;

import com.vermeg.collateralmanagement.enums.PortfolioType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioResponse {
    private Long id;
    private String name;
    private String description;
    private PortfolioType type;
    private BigDecimal totalValue;
    private BigDecimal totalMargin;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    // User information
    private Long userId;
    private String username;

    // Statistics
    private Integer assetCount;
    private Integer activeAssetCount;

    // Performance metrics
    private BigDecimal dailyChange;
    private Double dailyChangePercentage;
}