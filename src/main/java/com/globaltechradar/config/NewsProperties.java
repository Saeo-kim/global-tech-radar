package com.globaltechradar.config;

import com.globaltechradar.domain.NewsCategory;
import com.globaltechradar.domain.NewsSourceType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * {@code radar.news.*}: the news collector's configuration and the list of feeds registered at startup.
 * Feed credentials (if a feed ever needs one) belong in environment variables, never in templates or JS.
 */
@ConfigurationProperties(prefix = "radar.news")
public record NewsProperties(
    boolean collectionEnabled,
    Duration collectInterval,
    Duration initialDelay,
    Duration requestTimeout,
    int maxItemsPerFeed,
    int maxFeedBytes,
    int retentionDays,
    String userAgent,
    boolean allowPrivateHosts,
    /** auto | os | jdk — which certificate store validates feed TLS certificates. */
    String trustStore,
    List<Feed> feeds) {

    public NewsProperties {
        if (collectInterval == null) collectInterval = Duration.ofHours(1);
        if (initialDelay == null) initialDelay = Duration.ofSeconds(20);
        if (requestTimeout == null) requestTimeout = Duration.ofSeconds(20);
        if (maxItemsPerFeed <= 0) maxItemsPerFeed = 25;
        if (maxFeedBytes <= 0) maxFeedBytes = 5 * 1024 * 1024;
        if (retentionDays <= 0) retentionDays = 180;
        if (userAgent == null || userAgent.isBlank()) userAgent = "GlobalTechRadar/0.1 (RSS collector)";
        if (trustStore == null || trustStore.isBlank()) trustStore = "auto";
        if (feeds == null) feeds = List.of();
    }

    public record Feed(String id, String name, String url, NewsSourceType type, NewsCategory category,
                       String region, String company, Boolean enabled) {
        public boolean isEnabledByDefault() { return enabled == null || enabled; }
    }
}
