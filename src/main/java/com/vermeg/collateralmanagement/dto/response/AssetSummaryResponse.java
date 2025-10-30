// src/main/java/com/vermeg/collateralmanagement/dto/response/AssetSummaryResponse.java
package com.vermeg.collateralmanagement.dto.response;

import com.vermeg.collateralmanagement.dto.dashboard.AssetAllocationDto;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetSummaryResponse {
    private Integer totalAssets;
    private BigDecimal totalValue;
    private BigDecimal averageValue;
    private List<AssetAllocationDto> typeDistribution;
    private Map<String, Integer> statusDistribution;
    private Map<String, BigDecimal> currencyDistribution;
    private List<AssetResponse> topValuedAssets;
    private List<AssetResponse> recentlyAddedAssets;
    private Integer eligibleAssetsCount;
    private BigDecimal eligibleAssetsValue;
    private Integer nearMaturityCount;
    private BigDecimal nearMaturityValue;
}