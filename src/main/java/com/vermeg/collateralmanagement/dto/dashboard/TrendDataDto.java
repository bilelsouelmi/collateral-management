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
public class TrendDataDto {
    private LocalDateTime date;
    private BigDecimal value;
    private String label;
    private String category;
}