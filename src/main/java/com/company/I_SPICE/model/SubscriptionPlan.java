package com.company.I_SPICE.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 200)
    private String tagline;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal annualPrice;

    @Column(nullable = false, length = 120)
    private String valueLimitText;

    @Column(nullable = false, length = 80)
    private String iconClass;

    @Column(nullable = false, length = 120)
    private String buttonText;

    @Column(length = 120)
    private String badgeText;

    @Column(nullable = false)
    private boolean popular = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String featuresText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void normalize() {
        if (code != null) {
            code = code.trim().toUpperCase();
        }
        if (name != null) {
            name = name.trim();
        }
        if (tagline != null) {
            tagline = tagline.trim();
        }
        if (valueLimitText != null) {
            valueLimitText = valueLimitText.trim();
        }
        if (iconClass != null) {
            iconClass = iconClass.trim();
        }
        if (buttonText != null) {
            buttonText = buttonText.trim();
        }
        if (badgeText != null) {
            badgeText = badgeText.trim();
            if (badgeText.isEmpty()) {
                badgeText = null;
            }
        }
        if (featuresText != null) {
            featuresText = featuresText.trim();
        }
    }

    @Transient
    public List<String> getFeatures() {
        if (featuresText == null || featuresText.isBlank()) {
            return List.of();
        }
        return Arrays.stream(featuresText.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }
}
