// src/main/java/com/vermeg/collateralmanagement/dto/request/UpdateAssetRequest.java
package com.vermeg.collateralmanagement.dto.request;

import com.vermeg.collateralmanagement.enums.AssetType;
import com.vermeg.collateralmanagement.enums.AssetStatus;
import jakarta.validation.constraints.*;
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
public class UpdateAssetRequest {

    @Size(max = 200, message = "Asset name cannot exceed 200 characters")
    private String name;

    private AssetType type;

    @DecimalMin(value = "0.00", message = "Market value must be positive")
    private BigDecimal marketValue;

    @Size(max = 3, message = "Currency cannot exceed 3 characters")
    private String currency;

    @DecimalMin(value = "0.0000", message = "Haircut must be positive")
    @DecimalMax(value = "1.0000", message = "Haircut cannot exceed 100%")
    private BigDecimal haircut;

    private LocalDateTime dueDate;

    private Boolean isEligible;

    private AssetStatus status;

    private Long portfolioId;
}