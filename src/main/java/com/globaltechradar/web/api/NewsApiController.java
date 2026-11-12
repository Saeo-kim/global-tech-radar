package com.globaltechradar.web.api;

import com.globaltechradar.collector.NewsSourceRegistry;
import com.globaltechradar.domain.NewsCategory;
import com.globaltechradar.domain.NewsItem;
import com.globaltechradar.domain.NewsSource;
import com.globaltechradar.service.NewsAnalyzer;
import com.globaltechradar.service.NewsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * JSON API for news. Facts and interpretation are returned in separate objects.
 * Source records expose health information only — never credentials.
 */
@RestController
@RequestMapping("/api/news")
public class NewsApiController {

    private final NewsService news;
    private final NewsAnalyzer analyzer;
    private final NewsSourceRegistry registry;

    public NewsApiController(NewsService news, NewsAnalyzer analyzer, NewsSourceRegistry registry) {
        this.news = news;
        this.analyzer = analyzer;
        this.registry = registry;
    }

    public record SourceView(String id, String name, String url, String type, String defaultCategory, boolean enabled,
                             String origin, String lastStatus, Instant lastSuccessAt, Instant lastAttemptAt,
                             String lastError, int lastNewItems, long totalCollected) {
        static SourceView of(NewsSource s) {
            return new SourceView(s.getId(), s.getName(), s.getUrl(), s.getSourceType().name(), s.getDefaultCategory().name(),
                s.isEnabled(), s.getOrigin().name(), s.getLastStatus().name(), s.getLastSuccessAt(), s.getLastAttemptAt(),
                s.getLastError(), s.getLastNewItems(), s.getTotalCollected());
        }
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) NewsCategory category,
                                    @RequestParam(required = false) String region,
                                    @RequestParam(required = false) NewsService.StatusFilter status,
                                    @RequestParam(required = false) NewsService.SortOrder sort,
                                    @RequestParam(defaultValue = "1") int page) {
        NewsService.Page p = news.search(new NewsService.Filter(q, category, region, status, sort, page));
        NewsService.Stats stats = news.stats();
        return Map.of("items", p.items(), "total", p.total(), "page", p.page(), "pages", p.pages(),
            "liveCount", stats.live(), "sampleCount", stats.sample(),
            "lastSuccessfulCollection", stats.lastSuccessfulCollection() == null ? "" : stats.lastSuccessfulCollection().toString());
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        NewsItem item = news.get(id);
        return Map.of("facts", item, "interpretation", analyzer.analyse(item), "brief", analyzer.brief(item));
    }

    @GetMapping("/trending")
    public List<NewsService.Trend> trending() {
        return news.trending(15);
    }

    @GetMapping("/sources")
    public List<SourceView> sources() {
        return registry.all().stream().map(SourceView::of).toList();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }
}
