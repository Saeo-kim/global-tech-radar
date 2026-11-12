package com.globaltechradar.service;

import com.globaltechradar.domain.*;
import com.globaltechradar.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-side queries for countries, technologies, companies, the work market, trends and reports.
 */
@Service
@Transactional(readOnly = true)
public class IntelligenceService {

    /** The radar domains, in display order, with a one-line description each. */
    public static final Map<String, String> DOMAINS;

    static {
        Map<String, String> d = new LinkedHashMap<>();
        d.put("AI", "Foundation models, retrieval and on-device AI.");
        d.put("AI Agents", "Autonomous and semi-autonomous systems that plan and use tools.");
        d.put("Software Engineering", "Languages, runtimes and engineering practices.");
        d.put("Cloud", "Compute platforms, containers and sovereign infrastructure.");
        d.put("DevOps", "Delivery pipelines, platforms and operations.");
        d.put("Cybersecurity", "Identity, cryptography and supply-chain defence.");
        d.put("Data Engineering", "Storage, pipelines and retrieval infrastructure.");
        d.put("Robotics", "Industrial, mobile and humanoid robots.");
        d.put("Semiconductor", "Accelerators, packaging and process nodes.");
        d.put("Blockchain", "Distributed ledgers and tokenised assets.");
        d.put("Emerging Technologies", "Quantum, spatial computing and edge AI.");
        DOMAINS = Collections.unmodifiableMap(d);
    }

    private final CountryRepository countries;
    private final TechnologyRepository technologies;
    private final CompanyRepository companies;
    private final MarketSignalRepository marketSignals;
    private final TrendPointRepository trendPoints;
    private final IntelligenceReportRepository reports;

    public IntelligenceService(CountryRepository countries, TechnologyRepository technologies,
                               CompanyRepository companies, MarketSignalRepository marketSignals,
                               TrendPointRepository trendPoints, IntelligenceReportRepository reports) {
        this.countries = countries;
        this.technologies = technologies;
        this.companies = companies;
        this.marketSignals = marketSignals;
        this.trendPoints = trendPoints;
        this.reports = reports;
    }

    // ---------------------------------------------------------------- countries

    public List<Country> countries() {
        return countries.findAllByOrderByTechIndexDesc();
    }

    public Country country(String code) {
        return countries.findById(code.toUpperCase()).orElseThrow(() -> new NotFoundException("Country", code));
    }

    public Map<String, Country> countryIndex() {
        return countries().stream().collect(Collectors.toMap(Country::getCode, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public int rankOf(Country c) {
        List<Country> all = countries();
        for (int i = 0; i < all.size(); i++) if (all.get(i).getCode().equals(c.getCode())) return i + 1;
        return all.size();
    }

    // ---------------------------------------------------------------- technologies

    public List<Technology> technologies() {
        return technologies.findAllByOrderByMomentumDesc();
    }

    public Technology technology(String slug) {
        return technologies.findById(slug).orElseThrow(() -> new NotFoundException("Technology", slug));
    }

    public Map<String, Technology> technologyIndex() {
        return technologies().stream().collect(Collectors.toMap(Technology::getSlug, Function.identity()));
    }

    public List<Technology> technologiesInDomain(String domain) {
        return technologies().stream().filter(t -> domain.equalsIgnoreCase(t.getDomain())).toList();
    }

    /** Domain-level aggregates used by dashboard and radar overview cards. */
    public record DomainSummary(String name, String slug, String description, int technologyCount, int avgMomentum,
                                int avgDemand, double avgGrowth, double currentIndex, double indexChange,
                                Technology topTechnology) { }

    public List<DomainSummary> domainSummaries() {
        List<Technology> all = technologies();
        Map<String, List<TrendPoint>> trends = trendPoints.findBySeriesTypeOrderBySeriesKeyAscPeriodAsc(TrendSeriesType.TECHNOLOGY)
            .stream().collect(Collectors.groupingBy(TrendPoint::getSeriesKey));
        List<DomainSummary> out = new ArrayList<>();
        DOMAINS.forEach((name, description) -> {
            List<Technology> techs = all.stream().filter(t -> name.equals(t.getDomain())).toList();
            List<TrendPoint> series = trends.getOrDefault(name, List.of());
            double current = series.isEmpty() ? 0 : series.get(series.size() - 1).getValue();
            double previous = series.size() < 2 ? current : series.get(series.size() - 2).getValue();
            out.add(new DomainSummary(name, slugify(name), description, techs.size(),
                (int) Math.round(techs.stream().mapToInt(Technology::getMomentum).average().orElse(0)),
                (int) Math.round(techs.stream().mapToInt(Technology::getMarketDemand).average().orElse(0)),
                Math.round(techs.stream().mapToDouble(Technology::getGrowthRate).average().orElse(0) * 10) / 10.0,
                current, Math.round((current - previous) * 10) / 10.0,
                techs.stream().max(Comparator.comparingInt(Technology::getMomentum)).orElse(null)));
        });
        return out;
    }

    public static String slugify(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    public Optional<String> domainForSlug(String slug) {
        return DOMAINS.keySet().stream().filter(d -> slugify(d).equals(slug)).findFirst();
    }

    /** Technologies with the highest momentum outside the Adopt ring: the "emerging" list. */
    public List<Technology> emergingTechnologies(int limit) {
        return technologies().stream()
            .filter(t -> t.getRing() == RadarRing.TRIAL || t.getRing() == RadarRing.ASSESS)
            .sorted(Comparator.comparingDouble(Technology::getGrowthRate).reversed())
            .limit(limit).toList();
    }

    // ---------------------------------------------------------------- companies

    public List<Company> companies() {
        return companies.findAllByOrderByNameAsc();
    }

    public Company company(String slug) {
        return companies.findById(slug).orElseThrow(() -> new NotFoundException("Company", slug));
    }

    // ---------------------------------------------------------------- work market

    public List<MarketSignal> market(MarketCategory category) {
        return marketSignals.findByCategoryOrderByMetricValueDesc(category);
    }

    public Map<MarketCategory, List<MarketSignal>> marketByCategory() {
        Map<MarketCategory, List<MarketSignal>> map = new EnumMap<>(MarketCategory.class);
        for (MarketCategory c : MarketCategory.values()) map.put(c, market(c));
        return map;
    }

    public Optional<MarketSignal> salaryFor(String countryCode) {
        return market(MarketCategory.SALARY).stream().filter(s -> s.getRegion().equalsIgnoreCase(countryCode)).findFirst();
    }

    // ---------------------------------------------------------------- trends

    public TrendSeries trend(TrendSeriesType type) {
        return TrendSeries.of(trendPoints.findBySeriesTypeOrderBySeriesKeyAscPeriodAsc(type));
    }

    public TrendSeries trend(TrendSeriesType type, Collection<String> keys) {
        Set<String> wanted = new HashSet<>(keys);
        return TrendSeries.of(trendPoints.findBySeriesTypeOrderBySeriesKeyAscPeriodAsc(type).stream()
            .filter(p -> wanted.contains(p.getSeriesKey())).toList());
    }

    // ---------------------------------------------------------------- reports

    public List<IntelligenceReport> reports() {
        return reports.findAllByOrderByProvenancePublishedOnDescIdDesc();
    }

    public List<IntelligenceReport> latestReports(int limit) {
        return reports().stream().limit(limit).toList();
    }

    public List<IntelligenceReport> searchReports(String category, String region, String q) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        return reports().stream()
            .filter(r -> category == null || category.isBlank() || r.getCategory().equalsIgnoreCase(category))
            .filter(r -> region == null || region.isBlank() || r.getRegion().equalsIgnoreCase(region))
            .filter(r -> needle.isEmpty() || (r.getTitle() + " " + r.getSummary() + " " + r.getTags()).toLowerCase().contains(needle))
            .toList();
    }

    public List<String> reportCategories() {
        return reports().stream().map(IntelligenceReport::getCategory).distinct().sorted().toList();
    }

    public IntelligenceReport report(Long id) {
        return reports.findById(id).orElseThrow(() -> new NotFoundException("Report", id));
    }

    public List<IntelligenceReport> reportsMentioning(String technologySlug) {
        return reports().stream().filter(r -> r.getRelatedTechnologyList().contains(technologySlug)).toList();
    }

    public List<IntelligenceReport> reportsForRegion(String code) {
        return reports().stream().filter(r -> r.getRegion().equalsIgnoreCase(code)).toList();
    }

    // ---------------------------------------------------------------- search

    public record SearchHit(String type, String title, String subtitle, String url) { }

    public List<SearchHit> search(String q) {
        if (q == null || q.isBlank()) return List.of();
        String n = q.trim().toLowerCase();
        List<SearchHit> hits = new ArrayList<>();
        countries().stream().filter(c -> (c.getName() + " " + c.getCode() + " " + c.getStrengths()).toLowerCase().contains(n))
            .forEach(c -> hits.add(new SearchHit("Country", c.getFlag() + " " + c.getName(), c.getRegion(), "/countries/" + c.getCode())));
        technologies().stream().filter(t -> (t.getName() + " " + t.getDomain() + " " + t.getDescription()).toLowerCase().contains(n))
            .forEach(t -> hits.add(new SearchHit("Technology", t.getName(), t.getDomain() + " · " + t.getRing().getLabel(), "/technologies/" + t.getSlug())));
        companies().stream().filter(c -> (c.getName() + " " + c.getSector() + " " + c.getFocusAreas()).toLowerCase().contains(n))
            .forEach(c -> hits.add(new SearchHit("Company", c.getName(), c.getSector(), "/companies/" + c.getSlug())));
        reports().stream().filter(r -> (r.getTitle() + " " + r.getSummary() + " " + r.getTags()).toLowerCase().contains(n))
            .forEach(r -> hits.add(new SearchHit("Report", r.getTitle(), r.getCategory() + " · " + r.getProvenance().getPublishedOn(), "/reports/" + r.getId())));
        marketSignals.findAll().stream().filter(m -> m.getCategory() == MarketCategory.SKILL_DEMAND && m.getTitle().toLowerCase().contains(n))
            .forEach(m -> hits.add(new SearchHit("Skill", m.getTitle(), "Demand score " + Math.round(m.getMetricValue()), "/work-market#skills")));
        return hits;
    }
}
