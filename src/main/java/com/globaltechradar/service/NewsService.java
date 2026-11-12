package com.globaltechradar.service;

import com.globaltechradar.collector.NewsCollectionService;
import com.globaltechradar.domain.DataStatus;
import com.globaltechradar.domain.NewsCategory;
import com.globaltechradar.domain.NewsItem;
import com.globaltechradar.repository.NewsItemRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Read-side queries for "Latest Global Tech Updates": filtering, sorting, trending and highlights. */
@Service
@Transactional(readOnly = true)
public class NewsService {

    public static final int PAGE_SIZE = 12;
    /** Window for "trending" and "most important". Falls back to all items when the window is sparse. */
    public static final int RECENT_DAYS = 14;

    public enum StatusFilter { ALL, LIVE, SAMPLE }

    public enum SortOrder { NEWEST, OLDEST, IMPORTANCE }

    public record Filter(String q, NewsCategory category, String region, StatusFilter status, SortOrder sort, int page) {
        public Filter {
            if (status == null) status = StatusFilter.ALL;
            if (sort == null) sort = SortOrder.NEWEST;
            if (page < 1) page = 1;
            q = q == null ? "" : q.trim();
            region = region == null ? "" : region.trim();
        }

        public static Filter defaults() { return new Filter("", null, "", StatusFilter.ALL, SortOrder.NEWEST, 1); }
    }

    public record Page(List<NewsItem> items, int total, int page, int pages) { }

    public record Trend(String term, int count, String technologySlug) { }

    public record Stats(long live, long sample, Instant lastSuccessfulCollection, Instant lastRun) { }

    /** Curated terms counted for "Trending Technologies"; optional link to a radar technology. */
    private static final List<Object[]> TREND_TERMS = List.of(
        new Object[]{"AI agents", "agent|agents|agentic", "coding-agents"},
        new Object[]{"Large language models", "llm|llms|language model|language models", "foundation-models"},
        new Object[]{"Open-weight / open models", "open-weight|open weights|open model|open models", "foundation-models"},
        new Object[]{"Retrieval & RAG", "rag|retrieval", "rag"},
        new Object[]{"On-device / edge AI", "on-device|edge ai|npu", "edge-ai"},
        new Object[]{"GPUs & AI accelerators", "gpu|gpus|accelerator|accelerators|tpu|tpus", "ai-accelerators"},
        new Object[]{"Memory & HBM", "hbm|high-bandwidth memory|dram", "ai-accelerators"},
        new Object[]{"Chiplets & packaging", "chiplet|chiplets|advanced packaging", "chiplets"},
        new Object[]{"Humanoid robots", "humanoid|humanoids", "humanoid-robots"},
        new Object[]{"Robotics", "robot|robots|robotics", "amr"},
        new Object[]{"Quantum computing", "quantum|qubit|qubits", "quantum-computing"},
        new Object[]{"Post-quantum cryptography", "post-quantum|pqc", "post-quantum-crypto"},
        new Object[]{"Vulnerabilities & exploits", "vulnerability|vulnerabilities|cve|zero-day|exploited", "supply-chain-security"},
        new Object[]{"Kubernetes & containers", "kubernetes|container|containers", "kubernetes"},
        new Object[]{"Data centers & energy", "data center|data centers|datacenter|datacenters|power grid", "ai-accelerators"},
        new Object[]{"Open source", "open source|open-source", "typescript"},
        new Object[]{"Satellites", "satellite|satellites", null},
        new Object[]{"Moon & Mars missions", "moon|lunar|mars|artemis", null}
    );

    private static final List<Map.Entry<Object[], Pattern>> TREND_PATTERNS = TREND_TERMS.stream()
        .map(t -> Map.entry(t, Pattern.compile("(?<![\\w-])(" + t[1] + ")(?![\\w-])", Pattern.CASE_INSENSITIVE)))
        .toList();

    private final NewsItemRepository repository;
    private final SettingsService settings;

    public NewsService(NewsItemRepository repository, SettingsService settings) {
        this.repository = repository;
        this.settings = settings;
    }

    public List<NewsItem> all() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "publishedAt")).stream()
            .sorted(Comparator.comparing(NewsItem::getSortTime).reversed()).toList();
    }

    public NewsItem get(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("News item", id));
    }

    public Page search(Filter f) {
        String needle = f.q().toLowerCase(Locale.ROOT);
        Predicate<NewsItem> p = n -> true;
        if (!needle.isEmpty()) p = p.and(n -> (n.getTitle() + " " + nz(n.getSummary()) + " " + nz(n.getCompanies()) + " "
            + nz(n.getProvenance().getSourceName())).toLowerCase(Locale.ROOT).contains(needle));
        if (f.category() != null) p = p.and(n -> n.getCategory() == f.category());
        if (!f.region().isEmpty()) p = p.and(n -> f.region().equalsIgnoreCase(n.getRegion()));
        if (f.status() == StatusFilter.LIVE) p = p.and(NewsItem::isLive);
        if (f.status() == StatusFilter.SAMPLE) p = p.and(n -> !n.isLive());

        Comparator<NewsItem> order = switch (f.sort()) {
            case OLDEST -> Comparator.comparing(NewsItem::getSortTime);
            case IMPORTANCE -> Comparator.comparingInt(NewsItem::getImportance).reversed()
                .thenComparing(Comparator.comparing(NewsItem::getSortTime).reversed());
            case NEWEST -> Comparator.comparing(NewsItem::getSortTime).reversed();
        };
        List<NewsItem> matched = repository.findAll().stream().filter(p).sorted(order).toList();
        int pages = Math.max(1, (matched.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(f.page(), pages);
        int from = (page - 1) * PAGE_SIZE;
        return new Page(matched.subList(from, Math.min(matched.size(), from + PAGE_SIZE)), matched.size(), page, pages);
    }

    public List<NewsItem> latest(int limit) {
        return all().stream().limit(limit).toList();
    }

    /** Collected (live) items in the recent window first; sample items only fill gaps. */
    public List<NewsItem> mostImportant(int limit) {
        List<NewsItem> pool = recent();
        return pool.stream()
            .sorted(Comparator.comparing(NewsItem::isLive).reversed()
                .thenComparing(Comparator.comparingInt(NewsItem::getImportance).reversed())
                .thenComparing(Comparator.comparing(NewsItem::getSortTime).reversed()))
            .limit(limit).toList();
    }

    public List<Trend> trending(int limit) {
        List<NewsItem> pool = recent();
        List<Trend> out = new ArrayList<>();
        for (Map.Entry<Object[], Pattern> e : TREND_PATTERNS) {
            int n = (int) pool.stream().filter(i -> e.getValue().matcher(i.getTitle() + " " + nz(i.getSummary())).find()).count();
            if (n > 0) out.add(new Trend((String) e.getKey()[0], n, (String) e.getKey()[2]));
        }
        out.sort(Comparator.comparingInt(Trend::count).reversed());
        return out.stream().limit(limit).toList();
    }

    public Map<NewsCategory, Long> categoryCounts() {
        Map<NewsCategory, Long> m = new EnumMap<>(NewsCategory.class);
        for (NewsCategory c : NewsCategory.values()) m.put(c, 0L);
        repository.findAll().forEach(n -> { if (n.getCategory() != null) m.merge(n.getCategory(), 1L, Long::sum); });
        return m;
    }

    public List<String> regions() {
        return repository.findAll().stream().map(NewsItem::getRegion).filter(Objects::nonNull).distinct().sorted().toList();
    }

    public List<NewsItem> related(NewsItem item, int limit) {
        return all().stream().filter(n -> !n.getId().equals(item.getId()) && n.getCategory() == item.getCategory())
            .limit(limit).toList();
    }

    public Stats stats() {
        return new Stats(repository.countByProvenanceDataStatus(DataStatus.LIVE),
            repository.countByProvenanceDataStatus(DataStatus.SAMPLE),
            parse(settings.get(NewsCollectionService.LAST_COLLECTION_KEY)),
            parse(settings.get(NewsCollectionService.LAST_RUN_KEY)));
    }

    private List<NewsItem> recent() {
        Instant cutoff = Instant.now().minus(RECENT_DAYS, ChronoUnit.DAYS);
        List<NewsItem> all = all();
        List<NewsItem> recent = all.stream().filter(n -> n.getSortTime().isAfter(cutoff)).toList();
        return recent.size() >= 5 ? recent : all;
    }

    private static Instant parse(String s) {
        try { return s == null || s.isBlank() ? null : Instant.parse(s); } catch (RuntimeException e) { return null; }
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
