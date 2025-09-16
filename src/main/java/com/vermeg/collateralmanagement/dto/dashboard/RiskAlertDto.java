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
public class RiskAlertDto {
    private Long alertId;
    private String alertType;
    private String severity;
    private String message;
    private Long portfolioId;
    private String portfolioName;
    private LocalDateTime triggeredAt;
    private Boolean isRead;
}