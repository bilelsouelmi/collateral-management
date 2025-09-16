    package com.vermeg.collateralmanagement.entity;

    import com.vermeg.collateralmanagement.enums.PortfolioType;
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
    @Table(name = "portfolios")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @EntityListeners(AuditingEntityListener.class)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    public class Portfolio {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, length = 200)
        @NotBlank(message = "Portfolio name is required")
        @Size(max = 200, message = "Portfolio name cannot exceed 200 characters")
        private String name;

        @Column(length = 500)
        @Size(max = 500, message = "Description cannot exceed 500 characters")
        private String description;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private PortfolioType type;

        @Column(nullable = false, precision = 19, scale = 2)
        @NotNull(message = "Total value is required")
        @DecimalMin(value = "0.00", message = "Total value must be positive")
        @Builder.Default
        private BigDecimal totalValue = BigDecimal.ZERO;

        @Column(precision = 19, scale = 2)
        @Builder.Default
        private BigDecimal totalMargin = BigDecimal.ZERO;

        @CreatedDate
        @Column(nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @LastModifiedDate
        private LocalDateTime lastModifiedAt;

        // Relationships
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false)
        @JsonIgnoreProperties({"portfolios", "hibernateLazyInitializer", "handler"})
        private User user;

        @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        @JsonIgnoreProperties({"portfolio", "hibernateLazyInitializer", "handler"})
        private Set<CollateralAsset> assets;

        @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        @JsonIgnoreProperties({"portfolio", "hibernateLazyInitializer", "handler"})
        private Set<RiskMetric> riskMetrics;

        @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        @JsonIgnoreProperties({"portfolio", "hibernateLazyInitializer", "handler"})
        private Set<MarginCall> marginCalls;
    }