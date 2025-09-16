package com.vermeg.collateralmanagement.entity;

import com.vermeg.collateralmanagement.enums.ReportType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    @NotBlank(message = "Report name is required")
    @Size(max = 200, message = "Report name cannot exceed 200 characters")
    private String name;

    @Column(length = 500)
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType type;

    @Column
    private LocalDateTime generatedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"reports", "hibernateLazyInitializer", "handler"})
    private User user;

    // Business methods
    public boolean isGenerated() {
        return generatedAt != null;
    }

    public boolean isRecent(int hoursThreshold) {
        if (generatedAt == null) return false;
        return generatedAt.isAfter(LocalDateTime.now().minusHours(hoursThreshold));
    }

    public String getFileName() {
        return type.getFilePrefix() + id + "_" + createdAt.toLocalDate() + ".pdf";
    }
}