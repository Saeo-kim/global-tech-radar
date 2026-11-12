package com.globaltechradar.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * One technology news update.
 * <p>
 * Fields hold only what the original source reported (headline, summary, URL, date) plus
 * deterministic metadata derived from that text (category, region, companies, importance).
 * Interpretations are never stored here; they are produced on demand by {@code NewsAnalyzer}
 * and rendered separately from the facts.
 * <p>
 * {@code provenance.dataStatus} is LIVE only for articles actually collected from a registered
 * feed; bundled demo items are SAMPLE.
 */
@Entity
@Table(name = "news_item",
    uniqueConstraints = @UniqueConstraint(name = "uk_news_dedupe", columnNames = "dedupe_key"),
    indexes = {
        @Index(name = "idx_news_published", columnList = "published_at"),
        @Index(name = "idx_news_title_key", columnList = "title_key"),
        @Index(name = "idx_news_category", columnList = "category")
    })
public class NewsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500, nullable = false)
    private String title;

    /** SHA-256 of the normalised source URL (or of source+title when no URL exists). */
    @Column(name = "dedupe_key", length = 64, nullable = false)
    private String dedupeKey;

    /** Normalised title, used to catch the same story syndicated under different URLs. */
    @Column(name = "title_key", length = 300)
    private String titleKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private NewsCategory category;

    /** ISO country code (e.g. "KR"), "Europe" or "Global". */
    @Column(length = 32)
    private String region;

    @Column(length = 400)
    private String companies;        // comma separated

    /** Summary text as published by the source (HTML stripped, truncated). */
    @Column(length = 2000)
    private String summary;

    /** Original publication time reported by the feed. Null when the feed gave none. */
    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "collected_at")
    private Instant collectedAt;

    /** Registered feed id; null for sample items. */
    @Column(name = "feed_id", length = 64)
    private String feedId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NewsSourceType sourceType;

    /** 0-100, rule-based (source reliability, company mentions, signal keywords). */
    private int importance;

    @Embedded
    private DataProvenance provenance;

    protected NewsItem() { }

    public NewsItem(String title, String dedupeKey, String titleKey, NewsCategory category, String region,
                    String companies, String summary, Instant publishedAt, Instant collectedAt, String feedId,
                    NewsSourceType sourceType, int importance, DataProvenance provenance) {
        this.title = title;
        this.dedupeKey = dedupeKey;
        this.titleKey = titleKey;
        this.category = category;
        this.region = region;
        this.companies = companies;
        this.summary = summary;
        this.publishedAt = publishedAt;
        this.collectedAt = collectedAt;
        this.feedId = feedId;
        this.sourceType = sourceType;
        this.importance = importance;
        this.provenance = provenance;
    }

    /**
     * Re-applies derived metadata after classification rules change. Source facts (title, summary,
     * URL, dates, provenance) are never modified. Returns true when anything changed.
     */
    public boolean applyClassification(NewsCategory category, String region, String companies, int importance) {
        boolean changed = this.category != category || !java.util.Objects.equals(this.region, region)
            || !java.util.Objects.equals(this.companies, companies) || this.importance != importance;
        this.category = category;
        this.region = region;
        this.companies = companies;
        this.importance = importance;
        return changed;
    }

    /** Publication time if known, otherwise collection time. Used for sorting only. */
    public Instant getSortTime() {
        return publishedAt != null ? publishedAt : collectedAt != null ? collectedAt : Instant.EPOCH;
    }

    public LocalDate getPublishedDate() {
        return publishedAt == null ? null : publishedAt.atZone(ZoneOffset.UTC).toLocalDate();
    }

    public boolean isLive() { return provenance != null && !provenance.isSample(); }

    public List<String> getCompanyList() { return Csv.split(companies); }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDedupeKey() { return dedupeKey; }
    public String getTitleKey() { return titleKey; }
    public NewsCategory getCategory() { return category; }
    public String getRegion() { return region; }
    public String getCompanies() { return companies; }
    public String getSummary() { return summary; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCollectedAt() { return collectedAt; }
    public String getFeedId() { return feedId; }
    public NewsSourceType getSourceType() { return sourceType; }
    public int getImportance() { return importance; }
    public DataProvenance getProvenance() { return provenance; }
}
