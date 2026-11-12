package com.globaltechradar.service;

import com.globaltechradar.domain.AppSetting;
import com.globaltechradar.repository.AppSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/** Key/value user preferences stored in the database. */
@Service
public class SettingsService {

    public static final String DISPLAY_NAME = "profile.displayName";
    public static final String HOME_COUNTRY = "profile.homeCountry";
    public static final String TARGET_ROLE = "profile.targetRole";
    public static final String SHOW_SAMPLE_BANNER = "ui.showSampleBanner";
    public static final String ACCENT = "ui.accent";

    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put(DISPLAY_NAME, "Analyst");
        DEFAULTS.put(HOME_COUNTRY, "KR");
        DEFAULTS.put(TARGET_ROLE, "Backend / AI Engineer");
        DEFAULTS.put(SHOW_SAMPLE_BANNER, "true");
        DEFAULTS.put(ACCENT, "cyan");
    }

    private final AppSettingRepository repository;

    public SettingsService(AppSettingRepository repository) {
        this.repository = repository;
    }

    public String get(String key) {
        return repository.findById(key).map(AppSetting::getValue).orElse(DEFAULTS.get(key));
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    @Transactional
    public void put(String key, String value) {
        repository.findById(key).ifPresentOrElse(
            s -> s.setValue(value),
            () -> repository.save(new AppSetting(key, value)));
    }

    public Map<String, String> all() {
        Map<String, String> result = new LinkedHashMap<>(DEFAULTS);
        repository.findAll().forEach(s -> result.put(s.getKey(), s.getValue()));
        return result;
    }
}
