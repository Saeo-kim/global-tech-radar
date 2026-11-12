package com.globaltechradar.service;

import com.globaltechradar.config.RadarProperties;
import com.globaltechradar.domain.DataStatus;
import com.globaltechradar.provider.IntelligenceDataProvider;
import com.globaltechradar.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Loads intelligence records from the active {@link IntelligenceDataProvider} into the database.
 * User-owned data (skills, projects, goals, settings) is never touched by a reload.
 */
@Service
public class DataIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DataIngestionService.class);
    public static final String LAST_LOADED_KEY = "data.lastLoadedAt";

    private final List<IntelligenceDataProvider> providers;
    private final RadarProperties properties;
    private final CountryRepository countries;
    private final TechnologyRepository technologies;
    private final CompanyRepository companies;
    private final MarketSignalRepository marketSignals;
    private final TrendPointRepository trendPoints;
    private final IntelligenceReportRepository reports;
    private final LearningPathRepository learningPaths;
    private final SettingsService settings;
    private final NewsItemRepository news;

    public DataIngestionService(List<IntelligenceDataProvider> providers, RadarProperties properties,
                                CountryRepository countries, TechnologyRepository technologies,
                                CompanyRepository companies, MarketSignalRepository marketSignals,
                                TrendPointRepository trendPoints, IntelligenceReportRepository reports,
                                LearningPathRepository learningPaths, SettingsService settings,
                                NewsItemRepository news) {
        this.providers = providers;
        this.properties = properties;
        this.countries = countries;
        this.technologies = technologies;
        this.companies = companies;
        this.marketSignals = marketSignals;
        this.trendPoints = trendPoints;
        this.reports = reports;
        this.learningPaths = learningPaths;
        this.settings = settings;
        this.news = news;
    }

    public IntelligenceDataProvider activeProvider() {
        String id = properties.data().provider();
        return providers.stream()
            .filter(p -> p.id().equalsIgnoreCase(id))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No IntelligenceDataProvider with id '" + id
                + "'. Available: " + providers.stream().map(IntelligenceDataProvider::id).toList()));
    }

    public List<IntelligenceDataProvider> providers() {
        return providers;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedOnStartup() {
        if (!properties.data().seedOnStartup()) return;
        if (countries.count() == 0 || technologies.count() == 0) {
            int n = reload();
            log.info("Seeded {} intelligence records from provider '{}'", n, activeProvider().id());
        } else if (news.countByProvenanceDataStatus(DataStatus.SAMPLE) == 0) {
            // Databases created before the news feature existed: add only the sample news.
            int n = reloadSampleNews();
            if (n > 0) log.info("Seeded {} sample news items from provider '{}'", n, activeProvider().id());
        }
    }

    /**
     * Replaces sample news only. Collected (LIVE) news is never deleted here; it is managed by the
     * collector's retention policy.
     */
    @Transactional
    public int reloadSampleNews() {
        news.deleteByStatus(DataStatus.SAMPLE);
        return news.saveAll(activeProvider().sampleNews()).size();
    }

    /** Replace all intelligence records with a fresh load from the active provider. */
    @Transactional
    public int reload() {
        IntelligenceDataProvider p = activeProvider();
        trendPoints.deleteAllInBatch();
        marketSignals.deleteAllInBatch();
        reports.deleteAllInBatch();
        learningPaths.deleteAllInBatch();
        companies.deleteAllInBatch();
        technologies.deleteAllInBatch();
        countries.deleteAllInBatch();

        int n = 0;
        n += countries.saveAll(p.countries()).size();
        n += technologies.saveAll(p.technologies()).size();
        n += companies.saveAll(p.companies()).size();
        n += marketSignals.saveAll(p.marketSignals()).size();
        n += trendPoints.saveAll(p.trendPoints()).size();
        n += reports.saveAll(p.reports()).size();
        n += learningPaths.saveAll(p.learningPaths()).size();
        n += reloadSampleNews();
        settings.put(LAST_LOADED_KEY, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        return n;
    }
}
