package com.vermeg.collateralmanagement.enums;

import lombok.Getter;

@Getter
public enum AlertType {
    THRESHOLD_BREACH("Threshold Breach"),
    MARGIN_CALL("Margin Call"),
    PORTFOLIO_LIMIT("Portfolio Limit"),
    VALUATION_STALE("Valuation Stale"),
    RISK_LIMIT_EXCEEDED("Risk Limit Exceeded"),
    ASSET_MATURITY("Asset Maturity Warning"),
    SYSTEM_ERROR("System Error"),
    MARKET_VOLATILITY("Market Volatility Alert"),
    COMPLIANCE_VIOLATION("Compliance Violation"),
    CREDIT_RATING_CHANGE("Credit Rating Change");

    private final String displayName;

    AlertType(String displayName) {
        this.displayName = displayName;
    }

    public boolean isCritical() {
        return this == MARGIN_CALL ||
                this == RISK_LIMIT_EXCEEDED ||
                this == COMPLIANCE_VIOLATION ||
                this == SYSTEM_ERROR;
    }

    public boolean isRiskRelated() {
        return this == THRESHOLD_BREACH ||
                this == MARGIN_CALL ||
                this == PORTFOLIO_LIMIT ||
                this == RISK_LIMIT_EXCEEDED ||
                this == MARKET_VOLATILITY;
    }

    @Override
    public String toString() {
        return displayName;
    }
}