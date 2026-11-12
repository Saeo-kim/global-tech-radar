package com.globaltechradar.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Periodic collection. Active only when {@code radar.news.collection-enabled=true}
 * (off in tests so they never touch the network).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "radar.news", name = "collection-enabled", havingValue = "true")
public class NewsCollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsCollectionScheduler.class);

    private final NewsCollectionService collection;

    public NewsCollectionScheduler(NewsCollectionService collection) {
        this.collection = collection;
    }

    @Scheduled(initialDelayString = "${radar.news.initial-delay:PT20S}", fixedDelayString = "${radar.news.collect-interval:PT1H}")
    public void collect() {
        try {
            collection.collectAll();
        } catch (RuntimeException e) {
            log.error("Scheduled news collection failed", e);
        }
    }
}
