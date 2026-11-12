package com.globaltechradar.domain;

import jakarta.persistence.*;

import java.util.List;

/**
 * One data point of the global work market: a remote-job segment, a freelance rate,
 * a salary band, a skill-demand score or an international project opportunity.
 */
@Entity
@Table(name = "market_signal", indexes = @Index(name = "idx_market_category", columnList = "category"))
public class MarketSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private MarketCategory category;

    private String title;
    private String region;          // country code, region name or "Global"
    private String metricLabel;
    private double metricValue;
    private String unit;
    /** Optional second value, e.g. salary upper bound or YoY change. */
    private Double secondaryValue;

    @Column(length = 800)
    private String detail;

    @Column(length = 300)
    private String tags;

    @Embedded
    private DataProvenance provenance;

    protected MarketSignal() { }

    public MarketSignal(MarketCategory category, String title, String region, String metricLabel, double metricValue,
                        String unit, Double secondaryValue, String detail, String tags, DataProvenance provenance) {
        this.category = category;
        this.title = title;
        this.region = region;
        this.metricLabel = metricLabel;
        this.metricValue = metricValue;
        this.unit = unit;
        this.secondaryValue = secondaryValue;
        this.detail = detail;
        this.tags = tags;
        this.provenance = provenance;
    }

    public List<String> getTagList() { return Csv.split(tags); }

    public Long getId() { return id; }
    public MarketCategory getCategory() { return category; }
    public String getTitle() { return title; }
    public String getRegion() { return region; }
    public String getMetricLabel() { return metricLabel; }
    public double getMetricValue() { return metricValue; }
    public String getUnit() { return unit; }
    public Double getSecondaryValue() { return secondaryValue; }
    public String getDetail() { return detail; }
    public String getTags() { return tags; }
    public DataProvenance getProvenance() { return provenance; }
}
