package com.globaltechradar.provider;

import com.globaltechradar.domain.*;

import java.util.List;

/**
 * Source of intelligence records. The MVP ships with {@link SampleIntelligenceDataProvider}.
 * <p>
 * To integrate a live API, implement this interface (for example a {@code WorldBankCountryProvider}
 * that calls the World Bank Open Data API), register it as a Spring bean with a unique {@link #id()},
 * and set {@code radar.data.provider} to that id. Records produced by a live provider must use
 * {@link DataStatus#LIVE} provenance with a real source name, URL and publication date.
 * Providers may return an empty list for domains they do not cover.
 */
public interface IntelligenceDataProvider {

    String id();

    String displayName();

    DataStatus dataStatus();

    List<Country> countries();

    List<Technology> technologies();

    List<Company> companies();

    List<MarketSignal> marketSignals();

    List<TrendPoint> trendPoints();

    List<IntelligenceReport> reports();

    List<LearningPath> learningPaths();

    /**
     * Demo news items shown before (or alongside) collected news. Live news never comes from here;
     * it is collected from registered RSS/Atom feeds by the {@code collector} package.
     */
    default List<NewsItem> sampleNews() {
        return List.of();
    }
}
