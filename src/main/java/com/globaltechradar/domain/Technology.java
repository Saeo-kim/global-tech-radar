package com.globaltechradar.domain;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "technology")
public class Technology {

    @Id
    @Column(length = 64)
    private String slug;

    private String name;

    /** One of the radar domains (AI, AI Agents, Cloud, ...). See {@code TechnologyService.DOMAINS}. */
    @Column(length = 48)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private RadarQuadrant quadrant;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private RadarRing ring;

    private int maturity;       // 0-100
    private int momentum;       // 0-100
    private int marketDemand;   // 0-100
    private double growthRate;  // % YoY (sample)

    @Column(length = 1500)
    private String description;

    @Column(length = 800)
    private String keySignals;  // pipe separated

    @Column(length = 500)
    private String relatedSkills; // comma separated

    @Column(length = 300)
    private String leadingCountries; // comma separated ISO codes

    @Embedded
    private DataProvenance provenance;

    protected Technology() { }

    public Technology(String slug, String name, String domain, RadarQuadrant quadrant, RadarRing ring, int maturity,
                      int momentum, int marketDemand, double growthRate, String description, String keySignals,
                      String relatedSkills, String leadingCountries, DataProvenance provenance) {
        this.slug = slug;
        this.name = name;
        this.domain = domain;
        this.quadrant = quadrant;
        this.ring = ring;
        this.maturity = maturity;
        this.momentum = momentum;
        this.marketDemand = marketDemand;
        this.growthRate = growthRate;
        this.description = description;
        this.keySignals = keySignals;
        this.relatedSkills = relatedSkills;
        this.leadingCountries = leadingCountries;
        this.provenance = provenance;
    }

    public List<String> getKeySignalList() { return Csv.pipes(keySignals); }
    public List<String> getRelatedSkillList() { return Csv.split(relatedSkills); }
    public List<String> getLeadingCountryList() { return Csv.split(leadingCountries); }

    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getDomain() { return domain; }
    public RadarQuadrant getQuadrant() { return quadrant; }
    public RadarRing getRing() { return ring; }
    public int getMaturity() { return maturity; }
    public int getMomentum() { return momentum; }
    public int getMarketDemand() { return marketDemand; }
    public double getGrowthRate() { return growthRate; }
    public String getDescription() { return description; }
    public String getKeySignals() { return keySignals; }
    public String getRelatedSkills() { return relatedSkills; }
    public String getLeadingCountries() { return leadingCountries; }
    public DataProvenance getProvenance() { return provenance; }
}
