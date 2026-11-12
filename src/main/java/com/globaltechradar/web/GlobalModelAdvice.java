package com.globaltechradar.web;

import com.globaltechradar.provider.IntelligenceDataProvider;
import com.globaltechradar.service.DataIngestionService;
import com.globaltechradar.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/** Attributes every page needs: navigation state, data-provider status and profile basics. */
@ControllerAdvice(basePackageClasses = GlobalModelAdvice.class)
public class GlobalModelAdvice {

    public record NavItem(String path, String label, String icon) { }

    public static final List<NavItem> NAV = List.of(
        new NavItem("/", "Dashboard", "grid"),
        new NavItem("/countries", "Country Intelligence", "globe"),
        new NavItem("/technologies", "Technology Radar", "radar"),
        new NavItem("/companies", "Company Intelligence", "building"),
        new NavItem("/work-market", "Global Work Market", "briefcase"),
        new NavItem("/news", "Global Tech Updates", "news"),
        new NavItem("/position", "My Technology Position", "target"),
        new NavItem("/reports", "Intelligence Reports", "file"),
        new NavItem("/settings", "Settings", "settings")
    );

    private final DataIngestionService ingestion;
    private final SettingsService settings;

    public GlobalModelAdvice(DataIngestionService ingestion, SettingsService settings) {
        this.ingestion = ingestion;
        this.settings = settings;
    }

    @ModelAttribute("nav")
    public List<NavItem> nav() { return NAV; }

    @ModelAttribute("currentSection")
    public String currentSection(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || uri.equals("/")) return "/";
        return NAV.stream().map(NavItem::path).filter(p -> !p.equals("/") && uri.startsWith(p)).findFirst().orElse("");
    }

    @ModelAttribute("provider")
    public IntelligenceDataProvider provider() { return ingestion.activeProvider(); }

    @ModelAttribute("isSampleData")
    public boolean isSampleData() { return ingestion.activeProvider().dataStatus().name().equals("SAMPLE"); }

    @ModelAttribute("showSampleBanner")
    public boolean showSampleBanner() { return settings.getBoolean(SettingsService.SHOW_SAMPLE_BANNER); }

    @ModelAttribute("displayName")
    public String displayName() { return settings.get(SettingsService.DISPLAY_NAME); }

    @ModelAttribute("accent")
    public String accent() { return settings.get(SettingsService.ACCENT); }

    @ModelAttribute("lastLoadedAt")
    public String lastLoadedAt() { return settings.get(DataIngestionService.LAST_LOADED_KEY); }
}
