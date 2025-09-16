package com.vermeg.collateralmanagement.enums;

import lombok.Getter;

@Getter
public enum AssetType {
    CASH("Cash"),
    GOVERNMENT_BOND("Government Bond"),
    CORPORATE_BOND("Corporate Bond"),
    EQUITY("Equity"),
    COMMODITY("Commodity"),
    REAL_ESTATE("Real Estate");

    private final String displayName;

    AssetType(String displayName) {
        this.displayName = displayName;
    }

    public boolean isBond() {
        return this == GOVERNMENT_BOND || this == CORPORATE_BOND;
    }

    public boolean isFinancialInstrument() {
        return this == GOVERNMENT_BOND || this == CORPORATE_BOND || this == EQUITY;
    }

    public boolean isPhysicalAsset() {
        return this == COMMODITY || this == REAL_ESTATE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}