package com.globaltechradar.collector;

import com.globaltechradar.domain.NewsCategory;
import com.globaltechradar.domain.NewsSourceType;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Deterministic, explainable metadata derived from an article's own text:
 * category, region, mentioned companies, an importance score, and the keys used for de-duplication.
 * No external calls, no model inference.
 */
@Component
public class NewsClassifier {

    /** Companies recognised in headlines and summaries (case-sensitive, whole words). */
    static final List<String> COMPANIES = List.of(
        "Google", "DeepMind", "Microsoft", "Amazon", "AWS", "Apple", "NVIDIA", "Nvidia", "OpenAI", "Meta", "Samsung",
        "Alibaba", "Tencent", "TSMC", "Intel", "AMD", "Anthropic", "IBM", "Qualcomm", "Arm", "ASML", "SK hynix",
        "Micron", "Huawei", "Baidu", "ByteDance", "Sony", "Toyota", "Hyundai", "Tesla", "SpaceX", "Blue Origin",
        "NASA", "ESA", "JAXA", "Oracle", "Salesforce", "Cisco", "Cloudflare", "Databricks", "Snowflake", "GitHub",
        "Hugging Face", "Mistral", "Boston Dynamics", "Figure", "Siemens", "SAP", "Broadcom", "Palantir", "xAI");

    /** Canonical display names for aliases. */
    private static final Map<String, String> COMPANY_ALIASES = Map.of("Nvidia", "NVIDIA", "AWS", "Amazon", "DeepMind", "Google");

    private static final Map<String, Pattern> COMPANY_PATTERNS = COMPANIES.stream().collect(Collectors.toMap(
        c -> c, c -> Pattern.compile("(?<![\\w-])" + Pattern.quote(c) + "(?![\\w-])"), (a, b) -> a, LinkedHashMap::new));

    /** Region detection: first match wins, so specific countries come before "Europe". */
    private static final LinkedHashMap<String, List<String>> REGION_TERMS = new LinkedHashMap<>();

    static {
        REGION_TERMS.put("KR", List.of("south korea", "korea", "korean", "seoul"));
        REGION_TERMS.put("JP", List.of("japan", "japanese", "tokyo"));
        REGION_TERMS.put("CN", List.of("china", "chinese", "beijing", "shenzhen", "shanghai"));
        REGION_TERMS.put("IN", List.of("india", "indian", "bengaluru", "bangalore"));
        REGION_TERMS.put("SG", List.of("singapore", "singaporean"));
        REGION_TERMS.put("DE", List.of("germany", "german", "berlin", "munich"));
        REGION_TERMS.put("FR", List.of("france", "french", "paris"));
        REGION_TERMS.put("GB", List.of("united kingdom", "britain", "british", "london", "uk"));
        REGION_TERMS.put("US", List.of("united states", "u.s.", "us government", "washington", "silicon valley", "american"));
        REGION_TERMS.put("Europe", List.of("europe", "european", "eu"));
    }

    private static final Map<String, List<Pattern>> REGION_PATTERNS = new LinkedHashMap<>();
    private static final Map<NewsCategory, List<Pattern>> CATEGORY_PATTERNS = new EnumMap<>(NewsCategory.class);

    static {
        REGION_TERMS.forEach((code, terms) -> REGION_PATTERNS.put(code, terms.stream().map(NewsClassifier::word).toList()));
        for (NewsCategory c : NewsCategory.values()) CATEGORY_PATTERNS.put(c, c.getKeywords().stream().map(NewsClassifier::word).toList());
    }

    /** Words that usually signal a consequential announcement. */
    private static final List<Pattern> SIGNAL_TERMS = List.of(
        "launch", "launches", "launched", "release", "releases", "released", "announce", "announces", "announced",
        "introduce", "introduces", "unveil", "unveils", "breakthrough", "first", "record", "critical", "zero-day",
        "actively exploited", "acquire", "acquires", "acquisition", "regulation", "law", "funding", "partnership",
        "open-source", "open source", "general availability", "generally available").stream().map(NewsClassifier::word).toList();

    private static final Set<NewsCategory> HIGH_IMPACT = EnumSet.of(NewsCategory.AI, NewsCategory.AI_AGENTS,
        NewsCategory.SEMICONDUCTOR, NewsCategory.CYBERSECURITY);

    private static final Set<String> TRACKING_PARAMS = Set.of("utm_source", "utm_medium", "utm_campaign", "utm_term",
        "utm_content", "ref", "fbclid", "gclid", "mc_cid", "mc_eid", "cmpid");

    private static Pattern word(String term) {
        return Pattern.compile("(?<![\\w-])" + Pattern.quote(term) + "(?![\\w-])", Pattern.CASE_INSENSITIVE);
    }

    private static int count(List<Pattern> patterns, String text) {
        if (text == null || text.isEmpty()) return 0;
        int n = 0;
        for (Pattern p : patterns) if (p.matcher(text).find()) n++;
        return n;
    }

    /**
     * Highest keyword score wins (headline hits count 3x). The feed's default category gets a head start
     * and is scored first, so it also wins ties — e.g. a NASA post titled "Rocks and Chips" stays Space.
     */
    public NewsCategory category(String title, String summary, NewsCategory feedDefault) {
        List<NewsCategory> order = new ArrayList<>(List.of(NewsCategory.values()));
        if (feedDefault != null) { order.remove(feedDefault); order.add(0, feedDefault); }
        NewsCategory best = feedDefault;
        int bestScore = -1;
        for (NewsCategory c : order) {
            int score = count(CATEGORY_PATTERNS.get(c), title) * 3 + count(CATEGORY_PATTERNS.get(c), summary);
            if (c == feedDefault) score += 3;
            if (score > bestScore) { best = c; bestScore = score; }
        }
        // "AI" is the generic parent of "AI Agents": an agent term in the headline means the specific category.
        if (best == NewsCategory.AI && count(CATEGORY_PATTERNS.get(NewsCategory.AI_AGENTS), title) > 0) {
            best = NewsCategory.AI_AGENTS;
        }
        return best == null ? NewsCategory.EMERGING : best;
    }

    public String region(String title, String summary, String feedDefault) {
        String text = (title == null ? "" : title) + " " + (summary == null ? "" : summary);
        for (Map.Entry<String, List<Pattern>> e : REGION_PATTERNS.entrySet()) {
            for (Pattern p : e.getValue()) if (p.matcher(text).find()) return e.getKey();
        }
        return feedDefault == null || feedDefault.isBlank() ? "Global" : feedDefault;
    }

    public List<String> companies(String title, String summary, String publisherCompany) {
        String text = (title == null ? "" : title) + " " + (summary == null ? "" : summary);
        LinkedHashSet<String> found = new LinkedHashSet<>();
        if (publisherCompany != null && !publisherCompany.isBlank()) found.add(publisherCompany.trim());
        COMPANY_PATTERNS.forEach((name, p) -> {
            if (p.matcher(text).find()) found.add(COMPANY_ALIASES.getOrDefault(name, name));
        });
        return found.stream().limit(8).toList();
    }

    /**
     * 0-100. Source reliability (10-25) + signal words (max 25) + named companies (max 24)
     * + high-impact category (10) + headline/summary completeness (max 16).
     */
    public int importance(String title, String summary, NewsSourceType type, NewsCategory category, int companyCount) {
        int score = type == null ? 10 : type.getReliabilityWeight();
        score += Math.min(25, (count(SIGNAL_TERMS, title) * 2 + count(SIGNAL_TERMS, summary)) * 5);
        score += Math.min(24, companyCount * 6);
        if (HIGH_IMPACT.contains(category)) score += 10;
        if (summary != null && summary.length() > 120) score += 8;
        if (title != null && title.length() > 30) score += 8;
        return Math.max(0, Math.min(100, score));
    }

    // ------------------------------------------------------------------ de-duplication keys

    /** Lower-case scheme/host, drop fragment, tracking parameters and trailing slash. */
    public static String normaliseUrl(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            URI u = URI.create(url.trim());
            if (u.getHost() == null) return url.trim();
            String query = u.getRawQuery() == null ? null : Arrays.stream(u.getRawQuery().split("&"))
                .filter(p -> !p.isBlank() && !TRACKING_PARAMS.contains(p.split("=", 2)[0].toLowerCase()))
                .sorted().collect(Collectors.joining("&"));
            String path = u.getRawPath() == null || u.getRawPath().isEmpty() ? "/" : u.getRawPath();
            if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
            String host = u.getHost().toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) host = host.substring(4);
            return "https://" + host + (u.getPort() > 0 && u.getPort() != 80 && u.getPort() != 443 ? ":" + u.getPort() : "")
                + path + (query == null || query.isEmpty() ? "" : "?" + query);
        } catch (IllegalArgumentException e) {
            return url.trim();
        }
    }

    /** Lower-case, punctuation removed, whitespace collapsed; truncated to the column size. */
    public static String titleKey(String title) {
        if (title == null) return null;
        String k = title.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N} ]", " ").replaceAll("\\s+", " ").trim();
        return k.length() > 300 ? k.substring(0, 300) : k;
    }

    public static String sha256(String value) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
