package com.globaltechradar.domain;

public enum MarketCategory {
    REMOTE_JOBS("Remote Jobs"),
    FREELANCE("Freelance"),
    SALARY("Developer Salaries"),
    SKILL_DEMAND("In-demand Skills"),
    INTERNATIONAL_PROJECT("International Projects");

    private final String label;

    MarketCategory(String label) { this.label = label; }

    public String getLabel() { return label; }
}
