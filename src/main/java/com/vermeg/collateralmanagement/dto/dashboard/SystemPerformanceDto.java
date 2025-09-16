package com.vermeg.collateralmanagement.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemPerformanceDto {
    private Double responseTime; // milliseconds
    private Double throughput; // requests per second
    private Double errorRate; // percentage
    private Double uptime; // percentage
    private Integer activeUsers;
    private Integer concurrentSessions;
    private Long totalRequests;
    private Long successfulRequests;
}