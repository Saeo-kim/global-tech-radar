package com.globaltechradar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "radar")
public record RadarProperties(Data data, List<Integration> integrations) {

    public RadarProperties {
        if (data == null) data = new Data("sample", true);
        if (integrations == null) integrations = List.of();
    }

    public record Data(String provider, boolean seedOnStartup) {
        public Data {
            if (provider == null || provider.isBlank()) provider = "sample";
        }
    }

    /** A future live-data integration slot. Nothing is called while {@code enabled} is false. */
    public record Integration(String id, String name, String domain, String baseUrl, String apiKey, boolean enabled) {
        public boolean isConfigured() {
            return enabled && baseUrl != null && !baseUrl.isBlank();
        }

        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
