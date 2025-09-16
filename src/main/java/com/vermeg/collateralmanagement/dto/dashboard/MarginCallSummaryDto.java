package com.vermeg.collateralmanagement.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarginCallSummaryDto {
    private Integer totalMarginCalls;
    private Integer activeMarginCalls;
    private Integer overdueMarginCalls;
    private Integer acknowledgedMarginCalls;
    private BigDecimal totalShortfall;
    private BigDecimal averageShortfall;
    private List<MarginCallOverviewDto> recentMarginCalls;
    private List<UrgentMarginCallDto> urgentMarginCalls;
}