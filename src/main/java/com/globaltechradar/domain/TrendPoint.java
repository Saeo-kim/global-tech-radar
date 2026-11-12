package com.globaltechradar.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "trend_point", indexes = @Index(name = "idx_trend_series", columnList = "seriesType,seriesKey"))
public class TrendPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private TrendSeriesType seriesType;

    @Column(length = 64)
    private String seriesKey;

    @Column(name = "period_label", length = 16)
    private String period;

    @Column(name = "point_value")
    private double value;

    private String unit;

    @Embedded
    private DataProvenance provenance;

    protected TrendPoint() { }

    public TrendPoint(TrendSeriesType seriesType, String seriesKey, String period, double value, String unit,
                      DataProvenance provenance) {
        this.seriesType = seriesType;
        this.seriesKey = seriesKey;
        this.period = period;
        this.value = value;
        this.unit = unit;
        this.provenance = provenance;
    }

    public Long getId() { return id; }
    public TrendSeriesType getSeriesType() { return seriesType; }
    public String getSeriesKey() { return seriesKey; }
    public String getPeriod() { return period; }
    public double getValue() { return value; }
    public String getUnit() { return unit; }
    public DataProvenance getProvenance() { return provenance; }
}
