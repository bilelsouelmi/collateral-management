package com.vermeg.collateralmanagement.entity;

import com.vermeg.collateralmanagement.enums.MarginCallStatus;
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

@Entity
@Table(name = "margin_calls")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MarginCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Required margin is required")
    @DecimalMin(value = "0.00", message = "Required margin must be positive")
    private BigDecimal requiredMargin;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Current margin is required")
    @DecimalMin(value = "0.00", message = "Current margin must be positive")
    private BigDecimal currentMargin;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Shortfall is required")
    @DecimalMin(value = "0.00", message = "Shortfall must be positive")
    private BigDecimal shortfall;

    @Column
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MarginCallStatus status = MarginCallStatus.PENDING;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime lastModifiedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @JsonIgnoreProperties({"marginCalls", "hibernateLazyInitializer", "handler"})
    private Portfolio portfolio;

    // Business methods
    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) && status.isActive();
    }

    public boolean requiresAction() {
        return status.requiresAction();
    }

    public long getDaysUntilDue() {
        if (dueDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), dueDate);
    }
}