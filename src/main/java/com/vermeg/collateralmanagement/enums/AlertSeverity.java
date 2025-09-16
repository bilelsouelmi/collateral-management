package com.vermeg.collateralmanagement.enums;

import lombok.Getter;

@Getter
public enum AlertSeverity {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical");

    private final String displayName;

    AlertSeverity(String displayName) {
        this.displayName = displayName;
    }

    public boolean requiresImmediateAttention() {
        return this == HIGH || this == CRITICAL;
    }

    public boolean isCritical() {
        return this == CRITICAL;
    }

    @Override
    public String toString() {
        return displayName;
    }
}