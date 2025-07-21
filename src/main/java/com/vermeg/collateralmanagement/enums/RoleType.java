package com.vermeg.collateralmanagement.enums;

public enum RoleType {
    ADMINISTRATOR("Administrator"),
    RISK_OFFICER("Risk Officer"),
    MANAGER("Manager");

    private final String displayName;

    RoleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}