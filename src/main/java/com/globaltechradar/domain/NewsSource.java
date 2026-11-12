package com.globaltechradar.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A registered RSS / Atom feed and its collection health.
 * Feeds come from {@code radar.news.feeds} in configuration (origin CONFIG) or are added on the
 * News Sources page (origin USER).
 */
@Entity
@Table(name = "news_source")
public class NewsSource {

    public enum Origin { CONFIG, USER }

    public enum Status { NEVER, OK, ERROR }

    @Id
    @Column(length = 64)
    private String id;

    private String name;

    @Column(length = 500, nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NewsSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private NewsCategory defaultCategory;

    @Column(length = 32)
    private String defaultRegion;

    /** Company that publishes the feed (e.g. an official blog); always added to related companies. */
    @Column(length = 80)
    private String publisherCompany;

    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Origin origin = Origin.CONFIG;

    private Instant lastAttemptAt;
    private Instant lastSuccessAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Status lastStatus = Status.NEVER;

    @Column(length = 500)
    private String lastError;

    private int lastNewItems;
    private int consecutiveFailures;
    private long totalCollected;

    protected NewsSource() { }

    public NewsSource(String id, String name, String url, NewsSourceType sourceType, NewsCategory defaultCategory,
                      String defaultRegion, String publisherCompany, Origin origin) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.sourceType = sourceType;
        this.defaultCategory = defaultCategory;
        this.defaultRegion = defaultRegion;
        this.publisherCompany = publisherCompany;
        this.origin = origin;
    }

    /** Refresh descriptive fields from configuration; keeps the user's enabled flag and health stats. */
    public void updateFromConfig(String name, String url, NewsSourceType type, NewsCategory category, String region,
                                 String publisherCompany) {
        this.name = name;
        this.url = url;
        this.sourceType = type;
        this.defaultCategory = category;
        this.defaultRegion = region;
        this.publisherCompany = publisherCompany;
    }

    public void recordSuccess(Instant at, int newItems) {
        this.lastAttemptAt = at;
        this.lastSuccessAt = at;
        this.lastStatus = Status.OK;
        this.lastError = null;
        this.lastNewItems = newItems;
        this.consecutiveFailures = 0;
        this.totalCollected += newItems;
    }

    public void recordFailure(Instant at, String error) {
        this.lastAttemptAt = at;
        this.lastStatus = Status.ERROR;
        this.lastError = error == null ? "Unknown error" : error.length() > 480 ? error.substring(0, 480) : error;
        this.lastNewItems = 0;
        this.consecutiveFailures++;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getUrl() { return url; }
    public NewsSourceType getSourceType() { return sourceType; }
    public NewsCategory getDefaultCategory() { return defaultCategory; }
    public String getDefaultRegion() { return defaultRegion; }
    public String getPublisherCompany() { return publisherCompany; }
    public boolean isEnabled() { return enabled; }
    public Origin getOrigin() { return origin; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public Instant getLastSuccessAt() { return lastSuccessAt; }
    public Status getLastStatus() { return lastStatus; }
    public String getLastError() { return lastError; }
    public int getLastNewItems() { return lastNewItems; }
    public int getConsecutiveFailures() { return consecutiveFailures; }
    public long getTotalCollected() { return totalCollected; }
}
