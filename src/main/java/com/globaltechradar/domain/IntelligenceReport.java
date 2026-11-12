package com.globaltechradar.domain;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "intelligence_report")
public class IntelligenceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String category;     // e.g. "AI", "Policy", "Talent", "Semiconductor"
    private String region;       // country code or "Global"
    private int readMinutes;

    @Column(length = 600)
    private String summary;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(length = 300)
    private String tags;

    @Column(length = 300)
    private String relatedTechnologies; // technology slugs

    @Embedded
    private DataProvenance provenance;

    protected IntelligenceReport() { }

    public IntelligenceReport(String title, String category, String region, int readMinutes, String summary,
                              String body, String tags, String relatedTechnologies, DataProvenance provenance) {
        this.title = title;
        this.category = category;
        this.region = region;
        this.readMinutes = readMinutes;
        this.summary = summary;
        this.body = body;
        this.tags = tags;
        this.relatedTechnologies = relatedTechnologies;
        this.provenance = provenance;
    }

    public List<String> getTagList() { return Csv.split(tags); }
    public List<String> getRelatedTechnologyList() { return Csv.split(relatedTechnologies); }
    public List<String> getParagraphs() { return Csv.pipes(body); }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getRegion() { return region; }
    public int getReadMinutes() { return readMinutes; }
    public String getSummary() { return summary; }
    public String getBody() { return body; }
    public String getTags() { return tags; }
    public String getRelatedTechnologies() { return relatedTechnologies; }
    public DataProvenance getProvenance() { return provenance; }
}
