package com.vermeg.collateralmanagement.entity;

import com.vermeg.collateralmanagement.enums.AssetType;
import com.vermeg.collateralmanagement.enums.AssetStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "collateral_assets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CollateralAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Asset ID is required")
    @Size(max = 100, message = "Asset ID cannot exceed 100 characters")
    private String assetId;

    @Column(nullable = false, length = 200)
    @NotBlank(message = "Asset name is required")
    @Size(max = 200, message = "Asset name cannot exceed 200 characters")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AssetStatus status = AssetStatus.ACTIVE;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Market value is required")
    @DecimalMin(value = "0.00", message = "Market value must be positive")
    private BigDecimal marketValue;

    @Column(length = 3)
    @Size(max = 3, message = "Currency cannot exceed 3 characters")
    private String currency;

    @Column(precision = 5, scale = 4)
    @DecimalMin(value = "0.0000", message = "Haircut must be positive")
    @DecimalMax(value = "1.0000", message = "Haircut cannot exceed 100%")
    @Builder.Default
    private BigDecimal haircut = BigDecimal.ZERO;

    @Column
    private LocalDateTime dueDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isEligible = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime lastModifiedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @JsonIgnoreProperties({"assets", "hibernateLazyInitializer", "handler"})
    private Portfolio portfolio;

    @OneToMany(mappedBy = "collateralAsset", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"collateralAsset", "hibernateLazyInitializer", "handler"})
    private Set<Valuation> valuations;

    // Business methods
    public BigDecimal getAdjustedValue() {
        if (marketValue != null && haircut != null) {
            return marketValue.multiply(BigDecimal.ONE.subtract(haircut));
        }
        return marketValue != null ? marketValue : BigDecimal.ZERO;
    }

    public boolean isAvailableForPledging() {
        return status == AssetStatus.ACTIVE && isEligible;
    }

    public boolean isNearMaturity(int daysThreshold) {
        if (dueDate == null) return false;
        return dueDate.isBefore(LocalDateTime.now().plusDays(daysThreshold));
    }
}