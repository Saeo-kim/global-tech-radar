package com.globaltechradar.domain;

/** Whether a record came from the bundled sample dataset or from a live external source. */
public enum DataStatus {
    SAMPLE("Sample data"),
    LIVE("Live data");

    private final String label;

    DataStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
