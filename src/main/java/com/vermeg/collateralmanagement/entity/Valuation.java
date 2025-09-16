package com.vermeg.collateralmanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "valuations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Valuation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Value is required")
    @DecimalMin(value = "0.00", message = "Value must be positive")
    private BigDecimal value;

    @Column(length = 3)
    @Size(max = 3, message = "Currency cannot exceed 3 characters")
    private String currency;

    @Column
    private LocalDateTime valuationDate;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collateral_asset_id", nullable = false)
    @JsonIgnoreProperties({"valuations", "hibernateLazyInitializer", "handler"})
    private CollateralAsset collateralAsset;

    // Business methods
    public boolean isStale(int hoursThreshold) {
        if (valuationDate == null) return true;
        return valuationDate.isBefore(LocalDateTime.now().minusHours(hoursThreshold));
    }

    public boolean isCurrentValuation() {
        return !isStale(24); // Default: stale after 24 hours
    }
}