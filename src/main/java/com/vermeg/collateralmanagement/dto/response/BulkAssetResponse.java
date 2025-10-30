// src/main/java/com/vermeg/collateralmanagement/dto/response/BulkAssetResponse.java
package com.vermeg.collateralmanagement.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkAssetResponse {
    private Integer totalProcessed;
    private Integer successful;
    private Integer failed;
    private List<String> errors;
    private List<AssetResponse> processedAssets;
}