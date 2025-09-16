package com.vermeg.collateralmanagement.enums;

import lombok.Getter;

@Getter
public enum PortfolioType {
    EQUITY("Equity Portfolio"),
    FIXED_INCOME("Fixed Income Portfolio"),
    DERIVATIVES("Derivatives Portfolio"),
    COMMODITIES("Commodities Portfolio"),
    MIXED("Mixed Portfolio");

    private final String displayName;

    PortfolioType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}