package com.vermeg.collateralmanagement.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrgentMarginCallDto {
    private Long id;
    private String portfolioName;
    private BigDecimal shortfall;
    private LocalDateTime dueDate;
    private Long hoursUntilDue;
    private String urgencyLevel;
    private String recommendedAction;
}