package com.vermeg.collateralmanagement.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertTrendDataDto {
    private LocalDateTime date;
    private Integer totalAlerts;
    private Integer criticalAlerts;
    private Integer unreadAlerts;
}