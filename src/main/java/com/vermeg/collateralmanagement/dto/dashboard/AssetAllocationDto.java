package com.vermeg.collateralmanagement.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetAllocationDto {
    private String assetType;
    private String displayName;
    private BigDecimal value;
    private Double percentage;
    private Integer count;
    private String status;
}