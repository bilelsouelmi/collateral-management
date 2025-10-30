// src/main/java/com/vermeg/collateralmanagement/dto/request/AssetSearchRequest.java
package com.vermeg.collateralmanagement.dto.request;

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
public class AssetSearchRequest {
    private String search;
    private String assetId;
    private String name;
    private AssetType type;
    private AssetStatus status;
    private String currency;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private Long portfolioId;
    private Boolean isEligible;
    private String sortBy = "lastModifiedAt";
    private String sortDirection = "desc";
}