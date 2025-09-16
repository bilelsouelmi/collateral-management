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
public class MarginCallOverviewDto {
    private Long id;
    private Long portfolioId;
    private String portfolioName;
    private BigDecimal requiredMargin;
    private BigDecimal currentMargin;
    private BigDecimal shortfall;
    private String status;
    private LocalDateTime dueDate;
    private Boolean isOverdue;
    private Long daysUntilDue;
    private String urgencyLevel; // LOW, MEDIUM, HIGH, CRITICAL
}