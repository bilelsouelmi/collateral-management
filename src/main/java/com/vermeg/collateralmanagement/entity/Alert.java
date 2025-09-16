package com.vermeg.collateralmanagement.entity;

import com.vermeg.collateralmanagement.enums.AlertType;
import com.vermeg.collateralmanagement.enums.AlertSeverity;
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
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    @NotBlank(message = "Alert title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Column(length = 1000)
    @Size(max = 1000, message = "Message cannot exceed 1000 characters")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AlertSeverity severity = AlertSeverity.LOW;

    @Column
    private LocalDateTime triggeredAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"alerts", "hibernateLazyInitializer", "handler"})
    private User user;

    // Business methods
    public boolean isUnread() {
        return !isRead;
    }

    public boolean isCritical() {
        return severity.isCritical();
    }

    public boolean requiresImmediateAttention() {
        return severity.requiresImmediateAttention();
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public long getMinutesSinceTriggered() {
        if (triggeredAt == null) return 0;
        return java.time.temporal.ChronoUnit.MINUTES.between(triggeredAt, LocalDateTime.now());
    }
}