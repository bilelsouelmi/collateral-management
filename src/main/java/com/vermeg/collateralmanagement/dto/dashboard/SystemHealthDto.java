package com.vermeg.collateralmanagement.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthDto {
    private String overallStatus; // HEALTHY, WARNING, CRITICAL
    private Double healthScore; // 0-100
    private LocalDateTime lastChecked;
    private Map<String, String> componentStatus;
    private List<String> activeIssues;
    private List<String> recommendations;
    private SystemPerformanceDto performance;
}
