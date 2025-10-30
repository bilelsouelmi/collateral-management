package com.vermeg.collateralmanagement.dto.request;

import com.vermeg.collateralmanagement.enums.AssetType;
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
public class CreateAssetRequest {

    // ✅ OPTIONNEL - Sera généré automatiquement si vide
    @Size(max = 100, message = "Asset ID cannot exceed 100 characters")
    private String assetId;

    @NotBlank(message = "Asset name is required")
    @Size(max = 200, message = "Asset name cannot exceed 200 characters")
    private String name;

    @NotNull(message = "Asset type is required")
    private AssetType type;

    @NotNull(message = "Market value is required")
    @DecimalMin(value = "0.00", message = "Market value must be positive")
    private BigDecimal marketValue;

    @Size(max = 3, message = "Currency cannot exceed 3 characters")
    private String currency;

    @DecimalMin(value = "0.0000", message = "Haircut must be positive")
    @DecimalMax(value = "1.0000", message = "Haircut cannot exceed 100%")
    private BigDecimal haircut;

    private LocalDateTime dueDate;

    private Boolean isEligible = true;

    private Long portfolioId;
}