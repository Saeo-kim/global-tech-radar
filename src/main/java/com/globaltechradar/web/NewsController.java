package com.globaltechradar.web;

import com.globaltechradar.collector.FeedException;
import com.globaltechradar.collector.NewsCollectionService;
import com.globaltechradar.collector.NewsSourceRegistry;
import com.globaltechradar.domain.NewsCategory;
import com.globaltechradar.domain.NewsItem;
import com.globaltechradar.domain.NewsSourceType;
import com.globaltechradar.service.IntelligenceService;
import com.globaltechradar.service.NewsAnalyzer;
import com.globaltechradar.service.NewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** "Latest Global Tech Updates": list, detail and news-source management pages. */
@Controller
@RequestMapping("/news")
public class NewsController {

    private static final Logger log = LoggerFactory.getLogger(NewsController.class);

    private final NewsService news;
    private final NewsAnalyzer analyzer;
    private final NewsCollectionService collection;
    private final NewsSourceRegistry registry;
    private final IntelligenceService intel;

    public NewsController(NewsService news, NewsAnalyzer analyzer, NewsCollectionService collection,
                          NewsSourceRegistry registry, IntelligenceService intel) {
        this.news = news;
        this.analyzer = analyzer;
        this.collection = collection;
        this.registry = registry;
        this.intel = intel;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String q,
                       @RequestParam(name = "category", required = false) String category,
                       @RequestParam(name = "region", required = false) String region,
                       @RequestParam(name = "status", required = false) String status,
                       @RequestParam(name = "sort", required = false) String sort,
                       @RequestParam(name = "page", defaultValue = "1") int page,
                       Model model) {
        NewsService.Filter filter = new NewsService.Filter(q, parse(NewsCategory.class, category), region,
            parse(NewsService.StatusFilter.class, status), parse(NewsService.SortOrder.class, sort), page);
        NewsService.Page result = news.search(filter);
        List<NewsItem> important = news.mostImportant(3);

        model.addAttribute("filter", filter);
        model.addAttribute("result", result);
        model.addAttribute("briefs", briefs(result.items(), important));
        model.addAttribute("important", important);
        model.addAttribute("trending", news.trending(10));
        model.addAttribute("categoryCounts", news.categoryCounts());
        model.addAttribute("categories", NewsCategory.values());
        model.addAttribute("regions", regionOptions());
        model.addAttribute("stats", news.stats());
        model.addAttribute("collecting", collection.isRunning());
        model.addAttribute("countryIndex", intel.countryIndex());
        return "news/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        NewsItem item = news.get(id);
        model.addAttribute("item", item);
        model.addAttribute("brief", analyzer.brief(item));
        NewsAnalyzer.Analysis analysis = analyzer.analyse(item);
        Map<String, String> companySlugs = new LinkedHashMap<>();
        analysis.trackedCompanies().forEach(c -> companySlugs.put(c.getName(), c.getSlug()));
        model.addAttribute("analysis", analysis);
        model.addAttribute("companySlugs", companySlugs);
        model.addAttribute("related", news.related(item, 4));
        model.addAttribute("country", intel.countryIndex().get(item.getRegion()));
        model.addAttribute("source", item.getFeedId() == null ? null
            : registry.all().stream().filter(s -> s.getId().equals(item.getFeedId())).findFirst().orElse(null));
        return "news/detail";
    }

    // ------------------------------------------------------------------ collection & sources

    @PostMapping("/collect")
    public String collect(@RequestParam(name = "source", required = false) String sourceId,
                          @RequestParam(name = "back", defaultValue = "/news") String back, RedirectAttributes ra) {
        if (collection.isRunning()) {
            ra.addFlashAttribute("toast", "A collection run is already in progress.");
        } else {
            // Run in the background so the request returns immediately; progress is visible on the sources page.
            CompletableFuture.runAsync(() -> {
                try {
                    if (sourceId == null || sourceId.isBlank()) collection.collectAll();
                    else collection.collectOne(sourceId);
                } catch (RuntimeException e) {
                    log.error("Manual news collection failed", e);
                }
            });
            ra.addFlashAttribute("toast", sourceId == null || sourceId.isBlank()
                ? "Collection started for all enabled sources. Refresh in a moment."
                : "Collection started for this source. Refresh in a moment.");
        }
        return "redirect:" + (back.startsWith("/news") ? back : "/news");
    }

    @GetMapping("/sources")
    public String sources(Model model) {
        model.addAttribute("sources", registry.all());
        model.addAttribute("stats", news.stats());
        model.addAttribute("collecting", collection.isRunning());
        model.addAttribute("categories", NewsCategory.values());
        model.addAttribute("types", List.of(NewsSourceType.OFFICIAL_BLOG, NewsSourceType.RESEARCH,
            NewsSourceType.GOVERNMENT, NewsSourceType.MEDIA));
        model.addAttribute("regions", regionOptions());
        return "news/sources";
    }

    @PostMapping("/sources")
    public String addSource(@RequestParam String name, @RequestParam String url,
                            @RequestParam(defaultValue = "MEDIA") NewsSourceType type,
                            @RequestParam(defaultValue = "EMERGING") NewsCategory category,
                            @RequestParam(defaultValue = "Global") String region,
                            @RequestParam(required = false) String company,
                            RedirectAttributes ra) {
        try {
            var s = registry.register(name, url, type, category, region, company);
            ra.addFlashAttribute("toast", "Source \"" + s.getName() + "\" registered. Use \"Collect\" to fetch it now.");
        } catch (FeedException e) {
            ra.addFlashAttribute("toastError", "Could not register source: " + e.getMessage());
        }
        return "redirect:/news/sources";
    }

    @PostMapping("/sources/{id}/toggle")
    public String toggle(@PathVariable String id, RedirectAttributes ra) {
        var s = registry.get(id);
        registry.setEnabled(id, !s.isEnabled());
        ra.addFlashAttribute("toast", s.getName() + (s.isEnabled() ? " disabled." : " enabled."));
        return "redirect:/news/sources";
    }

    @PostMapping("/sources/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes ra) {
        if (registry.delete(id)) ra.addFlashAttribute("toast", "Source removed. Articles already collected are kept.");
        else ra.addFlashAttribute("toastError", "Configured sources can be disabled but not deleted (edit application.yml).");
        return "redirect:/news/sources";
    }

    // ------------------------------------------------------------------ helpers

    private Map<Long, NewsAnalyzer.Brief> briefs(List<NewsItem> a, List<NewsItem> b) {
        Map<Long, NewsAnalyzer.Brief> m = new LinkedHashMap<>();
        a.forEach(n -> m.put(n.getId(), analyzer.brief(n)));
        b.forEach(n -> m.putIfAbsent(n.getId(), analyzer.brief(n)));
        return m;
    }

    private Map<String, String> regionOptions() {
        Map<String, String> m = new LinkedHashMap<>();
        intel.countries().forEach(c -> m.put(c.getCode(), c.getFlag() + " " + c.getName()));
        m.put("Europe", "Europe");
        m.put("Global", "Global");
        news.regions().forEach(r -> m.putIfAbsent(r, r));
        return m;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String unknownSource(IllegalArgumentException e, RedirectAttributes ra) {
        ra.addFlashAttribute("toastError", e.getMessage());
        return "redirect:/news/sources";
    }
}
