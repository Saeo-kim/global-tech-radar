package com.globaltechradar.domain;

public enum RadarQuadrant {
    AI_DATA("AI & Data"),
    PLATFORMS("Platforms & Infrastructure"),
    ENGINEERING("Engineering & Security"),
    FRONTIER("Hardware & Frontier");

    private final String label;

    RadarQuadrant(String label) { this.label = label; }

    public String getLabel() { return label; }
}
