package com.vermeg.collateralmanagement.dto.response;

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
public class MarginCallResponse {
    private Long id;
    private BigDecimal requiredMargin;
    private BigDecimal currentMargin;
    private BigDecimal shortfall;
    private LocalDateTime dueDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private PortfolioInfo portfolio;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PortfolioInfo {
        private Long id;
        private String name;
        private UserInfo user;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private Long id;
        private String username;
        private String fullName;
    }
}