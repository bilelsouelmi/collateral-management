// src/main/java/com/vermeg/collateralmanagement/dto/response/AssetResponse.java
package com.vermeg.collateralmanagement.dto.response;

import com.vermeg.collateralmanagement.enums.AssetType;
import com.vermeg.collateralmanagement.enums.AssetStatus;
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
public class AssetResponse {
    private Long id;
    private String assetId;
    private String name;
    private AssetType type;
    private AssetStatus status;
    private BigDecimal marketValue;
    private String currency;
    private BigDecimal haircut;
    private LocalDateTime dueDate;
    private Boolean isEligible;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    // Portfolio information
    private Long portfolioId;
    private String portfolioName;

    // Calculated fields
    private BigDecimal adjustedValue;
    private Boolean availableForPledging;
    private Boolean nearMaturity;
    private Integer daysToMaturity;

    // Valuation information
    private LocalDateTime lastValuationDate;
    private BigDecimal previousValue;
    private BigDecimal valueChange;
    private Double valueChangePercentage;
}
