package com.vermeg.collateralmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Notification Preferences
    @Column(nullable = false)
    @Builder.Default
    private Boolean emailNotifications = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean alertNotifications = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean marginCallNotifications = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean reportNotifications = true;

    // Display Preferences
    @Column(length = 20)
    @Builder.Default
    private String theme = "light";

    @Column(length = 10)
    @Builder.Default
    private String language = "en";

    @Column(length = 20)
    @Builder.Default
    private String dateFormat = "DD/MM/YYYY";

    @Column(length = 10)
    @Builder.Default
    private String currency = "USD";

    // Dashboard Preferences
    @Column(length = 50)
    @Builder.Default
    private String defaultView = "dashboard";

    @Column(nullable = false)
    @Builder.Default
    private Boolean showWelcomeMessage = true;

    // Alert Thresholds - FIXED: Removed precision and scale for Double type
    @Column(nullable = false)
    @Builder.Default
    private Double riskAlertThreshold = 75.0;

    @Column(nullable = false)
    @Builder.Default
    private Double marginCallThreshold = 80.0;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime lastModifiedAt;
}