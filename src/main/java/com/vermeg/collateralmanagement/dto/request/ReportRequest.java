// ReportRequest.java
package com.vermeg.collateralmanagement.dto.request;

import com.vermeg.collateralmanagement.enums.ReportType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

/**
 * Request DTO for report generation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequest {

    @NotBlank(message = "Report name is required")
    @Size(max = 200, message = "Report name cannot exceed 200 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Report type is required")
    private ReportType type;

    private Long portfolioId;

    @Builder.Default
    private String format = "PDF";

    @Builder.Default
    private boolean generateImmediately = false;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;

    private String currency;
    private String riskThreshold;
    private Map<String, Object> parameters;

    @Builder.Default
    private boolean emailOnCompletion = false;
    private String emailRecipients;

    @Builder.Default
    private String priority = "MEDIUM";

    private List<String> tags;
    private String businessJustification;
}
