package com.globaltechradar.domain;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "country")
public class Country {

    @Id
    @Column(length = 2)
    private String code;              // ISO 3166-1 alpha-2

    private String name;
    private String region;
    private String flag;
    private double latitude;
    private double longitude;

    /** Composite technology index 0-100 (sample). */
    private int techIndex;
    private int aiReadiness;
    private int developerTalent;
    private int rndIntensity;
    private int startupEcosystem;
    private int digitalInfrastructure;
    private int remoteFriendliness;
    /** Estimated professional developers, in thousands (sample). */
    private int developersThousands;

    @Column(length = 500)
    private String strengths;         // comma separated

    @Column(length = 500)
    private String focusTechnologies; // comma separated technology slugs

    @Column(length = 1500)
    private String summary;

    @Embedded
    private DataProvenance provenance;

    protected Country() { }

    public Country(String code, String name, String region, String flag, double latitude, double longitude,
                   int techIndex, int aiReadiness, int developerTalent, int rndIntensity, int startupEcosystem,
                   int digitalInfrastructure, int remoteFriendliness, int developersThousands,
                   String strengths, String focusTechnologies, String summary, DataProvenance provenance) {
        this.code = code;
        this.name = name;
        this.region = region;
        this.flag = flag;
        this.latitude = latitude;
        this.longitude = longitude;
        this.techIndex = techIndex;
        this.aiReadiness = aiReadiness;
        this.developerTalent = developerTalent;
        this.rndIntensity = rndIntensity;
        this.startupEcosystem = startupEcosystem;
        this.digitalInfrastructure = digitalInfrastructure;
        this.remoteFriendliness = remoteFriendliness;
        this.developersThousands = developersThousands;
        this.strengths = strengths;
        this.focusTechnologies = focusTechnologies;
        this.summary = summary;
        this.provenance = provenance;
    }

    public List<String> getStrengthList() { return Csv.split(strengths); }
    public List<String> getFocusTechnologyList() { return Csv.split(focusTechnologies); }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getRegion() { return region; }
    public String getFlag() { return flag; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getTechIndex() { return techIndex; }
    public int getAiReadiness() { return aiReadiness; }
    public int getDeveloperTalent() { return developerTalent; }
    public int getRndIntensity() { return rndIntensity; }
    public int getStartupEcosystem() { return startupEcosystem; }
    public int getDigitalInfrastructure() { return digitalInfrastructure; }
    public int getRemoteFriendliness() { return remoteFriendliness; }
    public int getDevelopersThousands() { return developersThousands; }
    public String getStrengths() { return strengths; }
    public String getFocusTechnologies() { return focusTechnologies; }
    public String getSummary() { return summary; }
    public DataProvenance getProvenance() { return provenance; }
}
