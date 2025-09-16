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
@Table(name = "risk_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RiskMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Value is required")
    private BigDecimal value;

    @Column
    private LocalDateTime calculationDate;

    @Column(length = 100)
    @Size(max = 100, message = "Methodology cannot exceed 100 characters")
    private String methodology;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @JsonIgnoreProperties({"riskMetrics", "hibernateLazyInitializer", "handler"})
    private Portfolio portfolio;

    // Business methods
    public boolean isCurrentMetric() {
        if (calculationDate == null) return false;
        return calculationDate.isAfter(LocalDateTime.now().minusHours(24));
    }

    public boolean isValid() {
        return value != null && value.compareTo(BigDecimal.ZERO) >= 0;
    }
}