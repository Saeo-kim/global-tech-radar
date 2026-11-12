package com.globaltechradar.domain;

/** Kind of publisher behind a news feed. Drives a small reliability weight in importance scoring. */
public enum NewsSourceType {
    OFFICIAL_BLOG("Official company blog", 20),
    RESEARCH("Research institution / publication", 22),
    GOVERNMENT("Government / agency", 25),
    MEDIA("Technology media", 15),
    SAMPLE("Sample dataset", 10);

    private final String label;
    private final int reliabilityWeight;

    NewsSourceType(String label, int reliabilityWeight) {
        this.label = label;
        this.reliabilityWeight = reliabilityWeight;
    }

    public String getLabel() { return label; }
    public int getReliabilityWeight() { return reliabilityWeight; }
}
