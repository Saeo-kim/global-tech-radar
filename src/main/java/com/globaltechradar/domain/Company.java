package com.globaltechradar.domain;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "company")
public class Company {

    @Id
    @Column(length = 64)
    private String slug;

    private String name;
    private String headquartersCountry; // ISO code
    private String headquartersCity;
    private String sector;

    @Column(length = 500)
    private String focusAreas;      // comma separated

    @Column(length = 300)
    private String technologies;    // comma separated technology slugs

    private int aiInvestmentIndex;  // 0-100 (sample)
    private int talentDemandIndex;
    private int rndIndex;
    private int ecosystemIndex;
    private int hiringMomentum;

    @Column(length = 1500)
    private String summary;

    @Column(length = 1200)
    private String watchItems;      // pipe separated analyst watch items (sample)

    @Embedded
    private DataProvenance provenance;

    protected Company() { }

    public Company(String slug, String name, String headquartersCountry, String headquartersCity, String sector,
                   String focusAreas, String technologies, int aiInvestmentIndex, int talentDemandIndex,
                   int rndIndex, int ecosystemIndex, int hiringMomentum, String summary, String watchItems,
                   DataProvenance provenance) {
        this.slug = slug;
        this.name = name;
        this.headquartersCountry = headquartersCountry;
        this.headquartersCity = headquartersCity;
        this.sector = sector;
        this.focusAreas = focusAreas;
        this.technologies = technologies;
        this.aiInvestmentIndex = aiInvestmentIndex;
        this.talentDemandIndex = talentDemandIndex;
        this.rndIndex = rndIndex;
        this.ecosystemIndex = ecosystemIndex;
        this.hiringMomentum = hiringMomentum;
        this.summary = summary;
        this.watchItems = watchItems;
        this.provenance = provenance;
    }

    public int getCompositeScore() {
        return Math.round((aiInvestmentIndex + talentDemandIndex + rndIndex + ecosystemIndex + hiringMomentum) / 5f);
    }

    public List<String> getFocusAreaList() { return Csv.split(focusAreas); }
    public List<String> getTechnologyList() { return Csv.split(technologies); }
    public List<String> getWatchItemList() { return Csv.pipes(watchItems); }

    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getHeadquartersCountry() { return headquartersCountry; }
    public String getHeadquartersCity() { return headquartersCity; }
    public String getSector() { return sector; }
    public String getFocusAreas() { return focusAreas; }
    public String getTechnologies() { return technologies; }
    public int getAiInvestmentIndex() { return aiInvestmentIndex; }
    public int getTalentDemandIndex() { return talentDemandIndex; }
    public int getRndIndex() { return rndIndex; }
    public int getEcosystemIndex() { return ecosystemIndex; }
    public int getHiringMomentum() { return hiringMomentum; }
    public String getSummary() { return summary; }
    public String getWatchItems() { return watchItems; }
    public DataProvenance getProvenance() { return provenance; }
}
