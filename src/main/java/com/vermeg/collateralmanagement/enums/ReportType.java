package com.vermeg.collateralmanagement.enums;

import lombok.Getter;

@Getter
public enum ReportType {
    PORTFOLIO_SUMMARY("Portfolio Summary"),
    RISK_ANALYSIS("Risk Analysis"),
    MARGIN_REPORT("Margin Report"),
    COMPLIANCE_REPORT("Compliance Report"),
    VALUATION_REPORT("Valuation Report"),
    EXPOSURE_REPORT("Exposure Report"),
    PERFORMANCE_REPORT("Performance Report"),
    AUDIT_TRAIL("Audit Trail Report"),
    REGULATORY_FILING("Regulatory Filing"),
    STRESS_TEST_REPORT("Stress Test Report");

    private final String displayName;

    ReportType(String displayName) {
        this.displayName = displayName;
    }

    public boolean isRegulatoryRequired() {
        return this == COMPLIANCE_REPORT ||
                this == REGULATORY_FILING ||
                this == AUDIT_TRAIL;
    }

    public boolean isRiskRelated() {
        return this == RISK_ANALYSIS ||
                this == MARGIN_REPORT ||
                this == EXPOSURE_REPORT ||
                this == STRESS_TEST_REPORT;
    }

    public String getFilePrefix() {
        return this.name().toLowerCase() + "_";
    }

    @Override
    public String toString() {
        return displayName;
    }
}