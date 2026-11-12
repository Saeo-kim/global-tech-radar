package com.globaltechradar.domain;

/** Classic technology-radar rings, innermost first. */
public enum RadarRing {
    ADOPT("Adopt", "Proven and broadly recommended."),
    TRIAL("Trial", "Worth pursuing on projects that can handle some risk."),
    ASSESS("Assess", "Worth exploring to understand the impact."),
    HOLD("Hold", "Proceed with caution or wait for maturity.");

    private final String label;
    private final String description;

    RadarRing(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
}
