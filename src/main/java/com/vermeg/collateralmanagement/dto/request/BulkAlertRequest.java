package com.vermeg.collateralmanagement.dto.request;

import com.vermeg.collateralmanagement.enums.AlertType;
import com.vermeg.collateralmanagement.enums.AlertSeverity;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * Request DTO for bulk alert operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkAlertRequest {

    @NotEmpty(message = "At least one alert ID must be specified")
    private List<Long> alertIds;

    @Builder.Default
    private String operation = "MARK_READ"; // MARK_READ, DELETE, ARCHIVE

    // For bulk creation
    private List<Long> userIds;
    private AlertType type;
    private AlertSeverity severity;
    private String title;
    private String message;

    @Builder.Default
    private boolean continueOnError = true;

    @Builder.Default
    private Integer maxFailures = 10;

    public int getTotalAlertCount() {
        return alertIds != null ? alertIds.size() : 0;
    }

    public boolean isValidOperation() {
        return "MARK_READ".equals(operation) ||
                "DELETE".equals(operation) ||
                "ARCHIVE".equals(operation);
    }
}