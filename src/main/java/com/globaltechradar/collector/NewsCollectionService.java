package com.globaltechradar.collector;

import com.globaltechradar.config.NewsProperties;
import com.globaltechradar.domain.*;
import com.globaltechradar.repository.NewsItemRepository;
import com.globaltechradar.repository.NewsSourceRepository;
import com.globaltechradar.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates collection: fetch each enabled feed, parse, classify, de-duplicate and store.
 * A failing feed is recorded on its {@link NewsSource} row and never stops the other feeds.
 */
@Service
public class NewsCollectionService {

    private static final Logger log = LoggerFactory.getLogger(NewsCollectionService.class);
    public static final String LAST_COLLECTION_KEY = "news.lastSuccessfulCollectionAt";
    public static final String LAST_RUN_KEY = "news.lastRunAt";
    /** Same headline from another outlet within this window counts as a duplicate. */
    private static final long TITLE_DEDUPE_DAYS = 7;

    private final NewsSourceRegistry registry;
    private final NewsSourceRepository sourceRepository;
    private final NewsItemRepository items;
    private final FeedFetcher fetcher;
    private final FeedParser parser;
    private final NewsClassifier classifier;
    private final SettingsService settings;
    private final NewsProperties props;
    private final TransactionTemplate tx;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public NewsCollectionService(NewsSourceRegistry registry, NewsSourceRepository sourceRepository, NewsItemRepository items,
                                 FeedFetcher fetcher, FeedParser parser, NewsClassifier classifier,
                                 SettingsService settings, NewsProperties props, TransactionTemplate tx) {
        this.registry = registry;
        this.sourceRepository = sourceRepository;
        this.items = items;
        this.fetcher = fetcher;
        this.parser = parser;
        this.classifier = classifier;
        this.settings = settings;
        this.props = props;
        this.tx = tx;
    }

    public record SourceResult(String sourceId, String sourceName, boolean ok, int parsed, int added, int duplicates, String error) { }

    public record RunResult(Instant startedAt, Instant finishedAt, List<SourceResult> sources, boolean skipped) {
        public int added() { return sources.stream().mapToInt(SourceResult::added).sum(); }
        public long failed() { return sources.stream().filter(s -> !s.ok()).count(); }
        public long succeeded() { return sources.stream().filter(SourceResult::ok).count(); }
    }

    public boolean isRunning() { return running.get(); }

    /** Collects every enabled source. Returns {@code skipped=true} if a run is already in progress. */
    public RunResult collectAll() {
        return run(registry.enabled());
    }

    public RunResult collectOne(String sourceId) {
        return run(List.of(registry.get(sourceId)));
    }

    private RunResult run(List<NewsSource> targets) {
        Instant start = Instant.now();
        if (!running.compareAndSet(false, true)) {
            log.info("News collection already running; skipping this trigger");
            return new RunResult(start, start, List.of(), true);
        }
        try {
            List<SourceResult> results = new ArrayList<>();
            for (NewsSource s : targets) results.add(collectSource(s));
            Instant end = Instant.now();
            settings.put(LAST_RUN_KEY, end.toString());
            if (results.stream().anyMatch(SourceResult::ok)) settings.put(LAST_COLLECTION_KEY, end.toString());
            purgeOld();
            RunResult r = new RunResult(start, end, results, false);
            log.info("News collection finished: {} sources ok, {} failed, {} new items in {} ms",
                r.succeeded(), r.failed(), r.added(), end.toEpochMilli() - start.toEpochMilli());
            return r;
        } finally {
            running.set(false);
        }
    }

    SourceResult collectSource(NewsSource source) {
        List<FeedEntry> entries;
        try {
            entries = parser.parse(fetcher.fetch(source.getUrl()));
        } catch (FeedException e) {
            log.warn("News source '{}' failed: {}", source.getId(), e.getMessage());
            markFailure(source.getId(), e.getMessage());
            return new SourceResult(source.getId(), source.getName(), false, 0, 0, 0, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unexpected error collecting news source '{}'", source.getId(), e);
            markFailure(source.getId(), "Unexpected error: " + e.getClass().getSimpleName());
            return new SourceResult(source.getId(), source.getName(), false, 0, 0, 0, e.getClass().getSimpleName());
        }

        List<FeedEntry> limited = entries.stream()
            .sorted((a, b) -> compareDesc(a.publishedAt(), b.publishedAt()))
            .limit(props.maxItemsPerFeed()).toList();
        Instant now = Instant.now();
        int[] counts = new int[2]; // added, duplicates
        tx.executeWithoutResult(status -> {
            Set<String> seenThisRun = new HashSet<>();
            for (FeedEntry e : limited) {
                NewsItem item = toItem(source, e, now);
                if (!seenThisRun.add(item.getDedupeKey()) || items.existsByDedupeKey(item.getDedupeKey())
                    || items.existsByTitleKeyAndCollectedAtAfter(item.getTitleKey(), now.minus(TITLE_DEDUPE_DAYS, ChronoUnit.DAYS))) {
                    counts[1]++;
                    continue;
                }
                items.save(item);
                counts[0]++;
            }
            sourceRepository.findById(source.getId()).ifPresent(s -> s.recordSuccess(now, counts[0]));
        });
        log.debug("News source '{}': parsed {}, added {}, duplicates {}", source.getId(), entries.size(), counts[0], counts[1]);
        return new SourceResult(source.getId(), source.getName(), true, entries.size(), counts[0], counts[1], null);
    }

    NewsItem toItem(NewsSource source, FeedEntry e, Instant now) {
        String url = NewsClassifier.normaliseUrl(e.link());
        String dedupe = NewsClassifier.sha256(url != null ? url : source.getId() + "|" + e.title());
        NewsCategory category = classifier.category(e.title(), e.summary(), source.getDefaultCategory());
        List<String> companies = classifier.companies(e.title(), e.summary(), source.getPublisherCompany());
        String region = classifier.region(e.title(), e.summary(), source.getDefaultRegion());
        int importance = classifier.importance(e.title(), e.summary(), source.getSourceType(), category, companies.size());
        LocalDate published = e.publishedAt() == null ? null : e.publishedAt().atZone(ZoneOffset.UTC).toLocalDate();
        DataProvenance provenance = new DataProvenance(DataStatus.LIVE, source.getName(),
            e.link() == null ? null : truncate(e.link().trim(), 400), published, null);
        return new NewsItem(truncate(e.title(), 500), dedupe, NewsClassifier.titleKey(e.title()), category, region,
            truncate(String.join(",", companies), 400), e.summary() == null ? null : truncate(e.summary(), 2000),
            e.publishedAt(), now, source.getId(), source.getSourceType(), importance, provenance);
    }

    /**
     * Re-runs the (deterministic) classifier over stored collected items so that improved rules also
     * correct existing data. Only derived metadata changes; facts are untouched.
     */
    @Order(1)
    @EventListener(ApplicationReadyEvent.class)
    public int reclassifyCollected() {
        Integer changed = tx.execute(status -> {
            Map<String, NewsSource> byId = new HashMap<>();
            sourceRepository.findAll().forEach(s -> byId.put(s.getId(), s));
            int n = 0;
            for (NewsItem item : items.findAll()) {
                NewsSource s = item.getFeedId() == null ? null : byId.get(item.getFeedId());
                if (!item.isLive() || s == null) continue;
                NewsCategory category = classifier.category(item.getTitle(), item.getSummary(), s.getDefaultCategory());
                List<String> companies = classifier.companies(item.getTitle(), item.getSummary(), s.getPublisherCompany());
                String region = classifier.region(item.getTitle(), item.getSummary(), s.getDefaultRegion());
                int importance = classifier.importance(item.getTitle(), item.getSummary(), s.getSourceType(), category, companies.size());
                if (item.applyClassification(category, region, truncate(String.join(",", companies), 400), importance)) n++;
            }
            return n;
        });
        if (changed != null && changed > 0) log.info("Re-classified {} collected news items with current rules", changed);
        return changed == null ? 0 : changed;
    }

    private void markFailure(String sourceId, String message) {
        tx.executeWithoutResult(s -> sourceRepository.findById(sourceId).ifPresent(src -> src.recordFailure(Instant.now(), message)));
    }

    private void purgeOld() {
        Instant cutoff = Instant.now().minus(props.retentionDays(), ChronoUnit.DAYS);
        Integer n = tx.execute(s -> items.deleteLiveCollectedBefore(cutoff));
        if (n != null && n > 0) log.info("Purged {} collected news items older than {} days", n, props.retentionDays());
    }

    private static int compareDesc(Instant a, Instant b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return b.compareTo(a);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
