package com.globaltechradar.web;

import com.globaltechradar.domain.*;
import com.globaltechradar.service.IntelligenceService;
import com.globaltechradar.service.NotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Country Intelligence, Technology Radar, Company Intelligence, Work Market and Reports pages. */
@Controller
public class IntelligencePageController {

    private final IntelligenceService intel;

    public IntelligencePageController(IntelligenceService intel) {
        this.intel = intel;
    }

    // ------------------------------------------------------------ countries

    @GetMapping("/countries")
    public String countries(Model model) {
        model.addAttribute("countries", intel.countries());
        return "countries/list";
    }

    @GetMapping("/countries/{code}")
    public String country(@PathVariable String code, Model model) {
        Country c = intel.country(code);
        var techIndex = intel.technologyIndex();
        model.addAttribute("country", c);
        model.addAttribute("rank", intel.rankOf(c));
        model.addAttribute("total", intel.countries().size());
        model.addAttribute("countries", intel.countries());
        model.addAttribute("focusTechnologies", intel.domainSummaries().stream()
            .filter(d -> c.getFocusTechnologyList().contains(d.slug())).toList());
        model.addAttribute("leadingIn", techIndex.values().stream()
            .filter(t -> t.getLeadingCountryList().contains(c.getCode()))
            .sorted(Comparator.comparingInt(Technology::getMomentum).reversed()).toList());
        model.addAttribute("companies", intel.companies().stream()
            .filter(co -> co.getHeadquartersCountry().equals(c.getCode())).toList());
        model.addAttribute("salary", intel.salaryFor(c.getCode()).orElse(null));
        model.addAttribute("reports", intel.reportsForRegion(c.getCode()));
        return "countries/detail";
    }

    // ------------------------------------------------------------ technologies

    @GetMapping("/technologies")
    public String technologies(@RequestParam(name = "domain", required = false) String domain, Model model) {
        model.addAttribute("domains", intel.domainSummaries());
        model.addAttribute("technologies", intel.technologies());
        model.addAttribute("rings", RadarRing.values());
        model.addAttribute("quadrants", RadarQuadrant.values());
        model.addAttribute("selectedDomain", domain == null ? "" : domain);
        return "technologies/list";
    }

    @GetMapping("/technologies/domain/{slug}")
    public String domain(@PathVariable String slug) {
        String domain = intel.domainForSlug(slug).orElseThrow(() -> new NotFoundException("Domain", slug));
        return "redirect:/technologies?domain=" + java.net.URLEncoder.encode(domain, java.nio.charset.StandardCharsets.UTF_8);
    }

    @GetMapping("/technologies/{slug}")
    public String technology(@PathVariable String slug, Model model) {
        Technology t = intel.technology(slug);
        var countries = intel.countryIndex();
        model.addAttribute("tech", t);
        model.addAttribute("siblings", intel.technologiesInDomain(t.getDomain()).stream()
            .filter(o -> !o.getSlug().equals(slug)).toList());
        model.addAttribute("leadingCountries", t.getLeadingCountryList().stream().map(countries::get).filter(Objects::nonNull).toList());
        model.addAttribute("companies", intel.companies().stream().filter(c -> c.getTechnologyList().contains(slug)).toList());
        model.addAttribute("reports", intel.reportsMentioning(slug));
        model.addAttribute("domainSlug", IntelligenceService.slugify(t.getDomain()));
        return "technologies/detail";
    }

    // ------------------------------------------------------------ companies

    @GetMapping("/companies")
    public String companies(Model model) {
        List<Company> companies = intel.companies();
        model.addAttribute("companies", companies);
        model.addAttribute("ranked", companies.stream().sorted(Comparator.comparingInt(Company::getCompositeScore).reversed()).toList());
        return "companies/list";
    }

    @GetMapping("/companies/{slug}")
    public String company(@PathVariable String slug, Model model) {
        Company c = intel.company(slug);
        var techIndex = intel.technologyIndex();
        model.addAttribute("company", c);
        model.addAttribute("country", intel.countryIndex().get(c.getHeadquartersCountry()));
        model.addAttribute("technologies", c.getTechnologyList().stream().map(techIndex::get).filter(Objects::nonNull).toList());
        model.addAttribute("peers", intel.companies().stream().filter(o -> !o.getSlug().equals(slug))
            .sorted(Comparator.comparingInt((Company o) -> Math.abs(o.getCompositeScore() - c.getCompositeScore()))).limit(4).toList());
        return "companies/detail";
    }

    // ------------------------------------------------------------ work market

    @GetMapping("/work-market")
    public String workMarket(Model model) {
        var market = intel.marketByCategory();
        model.addAttribute("categories", MarketCategory.values());
        model.addAttribute("remote", market.get(MarketCategory.REMOTE_JOBS));
        model.addAttribute("freelance", market.get(MarketCategory.FREELANCE));
        model.addAttribute("salaries", market.get(MarketCategory.SALARY));
        model.addAttribute("skills", market.get(MarketCategory.SKILL_DEMAND));
        model.addAttribute("projects", market.get(MarketCategory.INTERNATIONAL_PROJECT));
        model.addAttribute("countryIndex", intel.countryIndex());
        return "work-market";
    }

    // ------------------------------------------------------------ reports

    @GetMapping("/reports")
    public String reports(@RequestParam(name = "category", required = false) String category,
                          @RequestParam(name = "region", required = false) String region,
                          @RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("reports", intel.searchReports(category, region, q));
        model.addAttribute("categories", intel.reportCategories());
        model.addAttribute("regions", intel.reports().stream().map(IntelligenceReport::getRegion).distinct().sorted().toList());
        model.addAttribute("category", category == null ? "" : category);
        model.addAttribute("region", region == null ? "" : region);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("total", intel.reports().size());
        return "reports/list";
    }

    @GetMapping("/reports/{id}")
    public String report(@PathVariable Long id, Model model) {
        IntelligenceReport r = intel.report(id);
        var techIndex = intel.technologyIndex();
        model.addAttribute("report", r);
        model.addAttribute("technologies", r.getRelatedTechnologyList().stream().map(techIndex::get).filter(Objects::nonNull).toList());
        model.addAttribute("country", intel.countryIndex().get(r.getRegion()));
        model.addAttribute("more", intel.reports().stream().filter(o -> !o.getId().equals(id)).limit(4).toList());
        return "reports/detail";
    }
}
