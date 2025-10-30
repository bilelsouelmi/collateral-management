package com.vermeg.collateralmanagement.dto.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskDistributionDto {
    private Integer low;
    private Integer medium;
    private Integer high;
    private Integer critical;
}