package com.vermeg.collateralmanagement.dto.request;

import com.vermeg.collateralmanagement.enums.AlertType;
import com.vermeg.collateralmanagement.enums.AlertSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Request DTO for creating manual alerts
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRequest {

    @NotBlank(message = "Alert title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Size(max = 1000, message = "Message cannot exceed 1000 characters")
    private String message;

    @NotNull(message = "Alert type is required")
    private AlertType type;

    @NotNull(message = "Alert severity is required")
    @Builder.Default
    private AlertSeverity severity = AlertSeverity.LOW;

    private Long targetUserId; // For admin creating alerts for specific users
}