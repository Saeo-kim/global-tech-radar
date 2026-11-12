package com.globaltechradar.web;

import com.globaltechradar.config.RadarProperties;
import com.globaltechradar.service.DataIngestionService;
import com.globaltechradar.service.IntelligenceService;
import com.globaltechradar.service.SettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private static final Set<String> ACCENTS = Set.of("cyan", "blue", "violet", "emerald");

    private final SettingsService settings;
    private final DataIngestionService ingestion;
    private final RadarProperties properties;
    private final IntelligenceService intel;

    public SettingsController(SettingsService settings, DataIngestionService ingestion, RadarProperties properties,
                              IntelligenceService intel) {
        this.settings = settings;
        this.ingestion = ingestion;
        this.properties = properties;
        this.intel = intel;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("settings", settings.all());
        model.addAttribute("providers", ingestion.providers());
        model.addAttribute("integrations", properties.integrations());
        model.addAttribute("countries", intel.countries());
        model.addAttribute("accents", ACCENTS.stream().sorted().toList());
        return "settings";
    }

    @PostMapping
    public String save(@RequestParam String displayName,
                       @RequestParam String homeCountry,
                       @RequestParam String targetRole,
                       @RequestParam(defaultValue = "cyan") String accent,
                       @RequestParam(defaultValue = "false") boolean showSampleBanner,
                       RedirectAttributes ra) {
        settings.put(SettingsService.DISPLAY_NAME, clip(displayName, 60, "Analyst"));
        settings.put(SettingsService.HOME_COUNTRY, clip(homeCountry, 2, "KR").toUpperCase());
        settings.put(SettingsService.TARGET_ROLE, clip(targetRole, 80, ""));
        settings.put(SettingsService.ACCENT, ACCENTS.contains(accent) ? accent : "cyan");
        settings.put(SettingsService.SHOW_SAMPLE_BANNER, Boolean.toString(showSampleBanner));
        ra.addFlashAttribute("toast", "Settings saved.");
        return "redirect:/settings";
    }

    @PostMapping("/reload-data")
    public String reload(RedirectAttributes ra) {
        int n = ingestion.reload();
        ra.addFlashAttribute("toast", "Reloaded " + n + " records from \"" + ingestion.activeProvider().displayName() + "\".");
        return "redirect:/settings";
    }

    private static String clip(String v, int max, String fallback) {
        if (v == null || v.isBlank()) return fallback;
        String t = v.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
