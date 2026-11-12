package com.globaltechradar.web.api;

import com.globaltechradar.domain.*;
import com.globaltechradar.provider.IntelligenceDataProvider;
import com.globaltechradar.service.DataIngestionService;
import com.globaltechradar.service.IntelligenceService;
import com.globaltechradar.service.PositionService;
import com.globaltechradar.service.TrendSeries;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * JSON API used by the dashboard charts. Also the integration surface for future clients.
 * Every payload that carries data also carries its provenance / dataStatus.
 */
@RestController
@RequestMapping("/api")
public class IntelligenceApiController {

    private final IntelligenceService intel;
    private final PositionService position;
    private final DataIngestionService ingestion;

    public IntelligenceApiController(IntelligenceService intel, PositionService position, DataIngestionService ingestion) {
        this.intel = intel;
        this.position = position;
        this.ingestion = ingestion;
    }

    @GetMapping("/meta")
    public Map<String, Object> meta() {
        IntelligenceDataProvider p = ingestion.activeProvider();
        return Map.of(
            "provider", p.id(),
            "providerName", p.displayName(),
            "dataStatus", p.dataStatus().name(),
            "notice", p.dataStatus() == DataStatus.SAMPLE
                ? "All values are illustrative sample data, not live measurements."
                : "Live data from configured providers.");
    }

    @GetMapping("/countries")
    public List<Country> countries() { return intel.countries(); }

    @GetMapping("/countries/{code}")
    public Country country(@PathVariable String code) { return intel.country(code); }

    @GetMapping("/technologies")
    public List<Technology> technologies() { return intel.technologies(); }

    @GetMapping("/domains")
    public List<IntelligenceService.DomainSummary> domains() { return intel.domainSummaries(); }

    @GetMapping("/companies")
    public List<Company> companies() { return intel.companies(); }

    @GetMapping("/market/{category}")
    public List<MarketSignal> market(@PathVariable String category) {
        return intel.market(MarketCategory.valueOf(category.toUpperCase().replace('-', '_')));
    }

    @GetMapping("/trends/{type}")
    public TrendSeries trends(@PathVariable String type, @RequestParam(name = "keys", required = false) String keys) {
        TrendSeriesType t = TrendSeriesType.valueOf(type.toUpperCase().replace('-', '_'));
        if (keys == null || keys.isBlank()) return intel.trend(t);
        return intel.trend(t, Arrays.stream(keys.split(",")).map(String::trim).toList());
    }

    @GetMapping("/reports")
    public List<IntelligenceReport> reports() { return intel.reports(); }

    @GetMapping("/position/analysis")
    public PositionService.Analysis analysis() { return position.analyse(); }

    @GetMapping("/search")
    public List<IntelligenceService.SearchHit> search(@RequestParam String q) { return intel.search(q); }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }
}
