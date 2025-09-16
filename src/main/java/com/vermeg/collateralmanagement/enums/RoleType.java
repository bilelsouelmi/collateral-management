package com.vermeg.collateralmanagement.enums;

import lombok.Getter;

@Getter
public enum RoleType {
    ADMINISTRATOR("Administrator"),
    RISK_OFFICER("Risk Officer"),
    MANAGER("Manager");

    private final String displayName;

    RoleType(String displayName) {
        this.displayName = displayName;
    }

    public boolean hasAdminPrivileges() {
        return this == ADMINISTRATOR;
    }

    public boolean canManageRisk() {
        return this == ADMINISTRATOR || this == RISK_OFFICER;
    }

    public boolean canViewReports() {
        return true; // All roles can view reports
    }

    @Override
    public String toString() {
        return displayName;
    }
}