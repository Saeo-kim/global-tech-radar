package com.globaltechradar.collector;

import com.globaltechradar.config.NewsProperties;
import com.globaltechradar.domain.NewsCategory;
import com.globaltechradar.domain.NewsSource;
import com.globaltechradar.domain.NewsSourceType;
import com.globaltechradar.repository.NewsSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Keeps the {@code news_source} table in sync with {@code radar.news.feeds} and lets users register
 * additional feeds at runtime. Adding a source therefore needs either one YAML entry or one form submit.
 */
@Service
public class NewsSourceRegistry {

    private static final Logger log = LoggerFactory.getLogger(NewsSourceRegistry.class);

    private final NewsSourceRepository sources;
    private final NewsProperties props;

    public NewsSourceRegistry(NewsSourceRepository sources, NewsProperties props) {
        this.sources = sources;
        this.props = props;
    }

    @Order(0)
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncConfiguredFeeds() {
        int added = 0;
        for (NewsProperties.Feed f : props.feeds()) {
            if (f.id() == null || f.url() == null) {
                log.warn("Skipping news feed with missing id or url: {}", f);
                continue;
            }
            NewsSourceType type = f.type() == null ? NewsSourceType.MEDIA : f.type();
            NewsCategory category = f.category() == null ? NewsCategory.EMERGING : f.category();
            String region = f.region() == null ? "Global" : f.region();
            var existing = sources.findById(f.id());
            if (existing.isPresent()) {
                existing.get().updateFromConfig(f.name(), f.url(), type, category, region, f.company());
            } else {
                NewsSource s = new NewsSource(f.id(), f.name(), f.url(), type, category, region, f.company(), NewsSource.Origin.CONFIG);
                s.setEnabled(f.isEnabledByDefault());
                sources.save(s);
                added++;
            }
        }
        log.info("News sources: {} configured, {} newly registered, {} total", props.feeds().size(), added, sources.count());
    }

    public List<NewsSource> all() {
        return sources.findAllByOrderByNameAsc();
    }

    public List<NewsSource> enabled() {
        return sources.findByEnabledTrue();
    }

    public NewsSource get(String id) {
        return sources.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown news source: " + id));
    }

    /** Validates and stores a user-registered feed. Throws {@link FeedException} with a user-facing message. */
    @Transactional
    public NewsSource register(String name, String url, NewsSourceType type, NewsCategory category, String region,
                               String publisherCompany) throws FeedException {
        if (name == null || name.isBlank()) throw new FeedException("Name is required");
        String cleanUrl = HttpFeedFetcher.validate(url, props.allowPrivateHosts()).toString();
        if (sources.existsByUrl(cleanUrl)) throw new FeedException("This feed URL is already registered");
        String base = slug(name);
        String id = base;
        for (int i = 2; sources.existsById(id); i++) id = base + "-" + i;
        NewsSource s = new NewsSource(id, name.trim(), cleanUrl, type == null ? NewsSourceType.MEDIA : type,
            category == null ? NewsCategory.EMERGING : category, region == null || region.isBlank() ? "Global" : region,
            publisherCompany == null || publisherCompany.isBlank() ? null : publisherCompany.trim(), NewsSource.Origin.USER);
        log.info("Registered news source '{}' ({})", id, cleanUrl);
        return sources.save(s);
    }

    @Transactional
    public void setEnabled(String id, boolean enabled) {
        get(id).setEnabled(enabled);
    }

    /** Only user-added sources can be deleted; configured ones can only be disabled. Collected items are kept. */
    @Transactional
    public boolean delete(String id) {
        NewsSource s = get(id);
        if (s.getOrigin() != NewsSource.Origin.USER) return false;
        sources.delete(s);
        return true;
    }

    static String slug(String s) {
        String slug = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.isEmpty()) slug = "feed";
        return slug.length() > 50 ? slug.substring(0, 50) : slug;
    }
}
