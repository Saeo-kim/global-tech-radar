package com.globaltechradar.domain;

public enum GoalStatus {
    PLANNED("Planned"),
    IN_PROGRESS("In progress"),
    ACHIEVED("Achieved");

    private final String label;

    GoalStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
