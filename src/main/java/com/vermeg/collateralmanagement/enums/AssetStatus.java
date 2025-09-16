package com.vermeg.collateralmanagement.enums;

import lombok.Getter;

@Getter
public enum AssetStatus {
    ACTIVE("Active"),
    PLEDGED("Pledged"),
    LIQUIDATED("Liquidated"),
    SUSPENDED("Suspended"),
    MATURED("Matured");

    private final String displayName;

    AssetStatus(String displayName) {
        this.displayName = displayName;
    }

    public boolean isAvailableForPledging() {
        return this == ACTIVE;
    }

    public boolean isCurrentlyPledged() {
        return this == PLEDGED;
    }

    public boolean isOperational() {
        return this == ACTIVE || this == PLEDGED;
    }

    @Override
    public String toString() {
        return displayName;
    }
}