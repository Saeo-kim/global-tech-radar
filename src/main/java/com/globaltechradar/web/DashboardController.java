package com.globaltechradar.web;

import com.globaltechradar.domain.*;
import com.globaltechradar.service.IntelligenceService;
import com.globaltechradar.service.NewsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;

@Controller
public class DashboardController {

    private final IntelligenceService intel;
    private final NewsService news;

    public DashboardController(IntelligenceService intel, NewsService news) {
        this.intel = intel;
        this.news = news;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        List<Country> countries = intel.countries();
        List<Technology> technologies = intel.technologies();
        List<MarketSignal> skills = intel.market(MarketCategory.SKILL_DEMAND);

        model.addAttribute("countries", countries);
        model.addAttribute("domains", intel.domainSummaries());
        model.addAttribute("emerging", intel.emergingTechnologies(6));
        model.addAttribute("latestReports", intel.latestReports(5));
        model.addAttribute("topSkills", skills.stream().limit(6).toList());
        model.addAttribute("fastestSkills", skills.stream()
            .sorted(Comparator.comparing((MarketSignal s) -> s.getSecondaryValue() == null ? 0 : s.getSecondaryValue()).reversed())
            .limit(4).toList());

        model.addAttribute("kpiCountries", countries.size());
        model.addAttribute("kpiAvgIndex", Math.round(countries.stream().mapToInt(Country::getTechIndex).average().orElse(0)));
        model.addAttribute("kpiTechnologies", technologies.size());
        model.addAttribute("kpiDevelopersMillions",
            Math.round(countries.stream().mapToInt(Country::getDevelopersThousands).sum() / 100.0) / 10.0);
        model.addAttribute("kpiTopTech", technologies.stream().max(Comparator.comparingDouble(Technology::getGrowthRate)).orElse(null));
        model.addAttribute("kpiLeader", countries.isEmpty() ? null : countries.get(0));
        model.addAttribute("remoteShare", intel.market(MarketCategory.REMOTE_JOBS).stream()
            .filter(s -> "Global".equals(s.getRegion())).mapToDouble(MarketSignal::getMetricValue).average().orElse(0));

        // Latest Global Tech Updates panel
        model.addAttribute("newsLatest", news.latest(6));
        model.addAttribute("newsTrending", news.trending(6));
        model.addAttribute("newsStats", news.stats());
        return "dashboard";
    }

    @GetMapping("/search")
    public String search(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("hits", intel.search(q));
        return "search";
    }
}
