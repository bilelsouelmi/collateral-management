// src/main/java/com/vermeg/collateralmanagement/dto/request/CreatePortfolioRequest.java
package com.vermeg.collateralmanagement.dto.request;

import com.vermeg.collateralmanagement.enums.PortfolioType;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePortfolioRequest {

    @NotBlank(message = "Portfolio name is required")
    @Size(max = 200, message = "Portfolio name cannot exceed 200 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Portfolio type is required")
    private PortfolioType type;
}
