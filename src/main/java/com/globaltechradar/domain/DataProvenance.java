package com.globaltechradar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;

/**
 * Where a record came from. Every intelligence record carries one so the UI can always
 * show the source, the publication date and whether the value is sample or live.
 */
@Embeddable
public class DataProvenance {

    @Enumerated(EnumType.STRING)
    @Column(name = "data_status", length = 16)
    private DataStatus dataStatus = DataStatus.SAMPLE;

    @Column(name = "source_name", length = 160)
    private String sourceName;

    @Column(name = "source_url", length = 400)
    private String sourceUrl;

    @Column(name = "published_on")
    private LocalDate publishedOn;

    /** For sample records: the real-world source a future live integration would use. */
    @Column(name = "planned_source", length = 200)
    private String plannedSource;

    protected DataProvenance() { }

    public DataProvenance(DataStatus dataStatus, String sourceName, String sourceUrl,
                          LocalDate publishedOn, String plannedSource) {
        this.dataStatus = dataStatus;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.publishedOn = publishedOn;
        this.plannedSource = plannedSource;
    }

    public static DataProvenance sample(String sourceName, LocalDate compiledOn, String plannedSource) {
        return new DataProvenance(DataStatus.SAMPLE, sourceName, null, compiledOn, plannedSource);
    }

    public boolean isSample() { return dataStatus == null || dataStatus == DataStatus.SAMPLE; }

    public DataStatus getDataStatus() { return dataStatus; }
    public String getSourceName() { return sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public LocalDate getPublishedOn() { return publishedOn; }
    public String getPlannedSource() { return plannedSource; }
}
