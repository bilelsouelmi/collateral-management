package com.vermeg.collateralmanagement.enums;

import lombok.Getter;

@Getter
public enum MarginCallStatus {
    PENDING("Pending"),
    ACKNOWLEDGED("Acknowledged"),
    RESOLVED("Resolved"),
    OVERDUE("Overdue");

    private final String displayName;

    MarginCallStatus(String displayName) {
        this.displayName = displayName;
    }

    public boolean requiresAction() {
        return this == PENDING || this == OVERDUE;
    }

    public boolean isResolved() {
        return this == RESOLVED;
    }

    public boolean isActive() {
        return !isResolved();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
