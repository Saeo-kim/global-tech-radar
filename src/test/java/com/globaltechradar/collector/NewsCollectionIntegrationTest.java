package com.globaltechradar.collector;

import com.globaltechradar.domain.*;
import com.globaltechradar.repository.NewsItemRepository;
import com.globaltechradar.repository.NewsSourceRepository;
import com.globaltechradar.service.DataIngestionService;
import com.globaltechradar.service.NewsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/** Full collection pipeline against canned feeds — no network access. */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:gtr-collect;MODE=MySQL;DB_CLOSE_DELAY=-1")
class NewsCollectionIntegrationTest {

    static final Map<String, String> FEEDS = new ConcurrentHashMap<>();

    @TestConfiguration
    static class CannedFeeds {
        @Bean
        @Primary
        FeedFetcher cannedFetcher() {
            return url -> {
                String body = FEEDS.get(url);
                if (body == null) throw new FeedException("HTTP 404 from test host");
                return body.getBytes(StandardCharsets.UTF_8);
            };
        }
    }

    static final String GOOD = """
        <rss version="2.0"><channel><title>Good</title>
          <item><title>NVIDIA unveils new GPU for data centers</title>
            <link>https://good.example/gpu?utm_source=rss</link>
            <description>The chip targets AI training in the United States.</description>
            <pubDate>Mon, 09 Nov 2026 08:30:00 +0000</pubDate></item>
          <item><title>Humanoid robot pilot in Seoul factory</title>
            <link>https://good.example/robot</link>
            <description>A robotics firm reported results.</description>
            <pubDate>Sun, 08 Nov 2026 08:30:00 +0000</pubDate></item>
          <item><title>NVIDIA unveils new GPU for data centers</title>
            <link>https://good.example/gpu/</link>
            <description>Same story, duplicate URL variant.</description></item>
        </channel></rss>""";

    @Autowired NewsCollectionService collection;
    @Autowired NewsSourceRepository sources;
    @Autowired NewsItemRepository items;
    @Autowired NewsService news;
    @Autowired DataIngestionService ingestion;

    @BeforeEach
    void setUp() {
        items.deleteAll();
        sources.deleteAll();
        FEEDS.clear();
        FEEDS.put("https://good.example/feed", GOOD);
        FEEDS.put("https://broken.example/feed", "<rss><channel><item><title>oops");
        sources.save(new NewsSource("good", "Good Feed", "https://good.example/feed", NewsSourceType.OFFICIAL_BLOG,
            NewsCategory.SEMICONDUCTOR, "US", "NVIDIA", NewsSource.Origin.CONFIG));
        sources.save(new NewsSource("broken", "Broken Feed", "https://broken.example/feed", NewsSourceType.MEDIA,
            NewsCategory.AI, "Global", null, NewsSource.Origin.USER));
        sources.save(new NewsSource("missing", "Missing Feed", "https://missing.example/feed", NewsSourceType.MEDIA,
            NewsCategory.AI, "Global", null, NewsSource.Origin.USER));
    }

    @Test
    void collectsClassifiesAndPreservesOriginalUrlAndDate() {
        NewsCollectionService.RunResult r = collection.collectAll();

        assertThat(r.skipped()).isFalse();
        assertThat(r.added()).isEqualTo(2);           // third item is a duplicate of the first
        assertThat(r.succeeded()).isEqualTo(1);
        assertThat(r.failed()).isEqualTo(2);

        List<NewsItem> live = items.findByFeedIdOrderByPublishedAtDesc("good");
        assertThat(live).hasSize(2).allSatisfy(n -> {
            assertThat(n.isLive()).isTrue();
            assertThat(n.getProvenance().getDataStatus()).isEqualTo(DataStatus.LIVE);
            assertThat(n.getProvenance().getSourceName()).isEqualTo("Good Feed");
        });
        NewsItem gpu = live.get(0);
        assertThat(gpu.getProvenance().getSourceUrl()).isEqualTo("https://good.example/gpu?utm_source=rss");
        assertThat(gpu.getPublishedAt()).isEqualTo(Instant.parse("2026-11-09T08:30:00Z"));
        assertThat(gpu.getCategory()).isEqualTo(NewsCategory.SEMICONDUCTOR);
        assertThat(gpu.getCompanyList()).contains("NVIDIA");
        assertThat(gpu.getRegion()).isEqualTo("US");
        NewsItem robot = live.get(1);
        assertThat(robot.getCategory()).isEqualTo(NewsCategory.ROBOTICS);
        assertThat(robot.getRegion()).isEqualTo("KR");

        assertThat(news.stats().lastSuccessfulCollection()).isNotNull();
    }

    @Test
    void secondRunAddsNoDuplicates() {
        collection.collectAll();
        NewsCollectionService.RunResult again = collection.collectAll();
        assertThat(again.added()).isZero();
        assertThat(items.findByFeedIdOrderByPublishedAtDesc("good")).hasSize(2);
    }

    @Test
    void reclassificationIsDeterministicAndKeepsFacts() {
        collection.collectAll();
        NewsItem before = items.findByFeedIdOrderByPublishedAtDesc("good").get(0);
        assertThat(collection.reclassifyCollected()).isZero();   // same rules -> nothing changes
        NewsItem after = items.findById(before.getId()).orElseThrow();
        assertThat(after.getTitle()).isEqualTo(before.getTitle());
        assertThat(after.getProvenance().getSourceUrl()).isEqualTo(before.getProvenance().getSourceUrl());
        assertThat(after.getPublishedAt()).isEqualTo(before.getPublishedAt());
    }

    @Test
    void failingFeedsAreRecordedWithoutStoppingOthers() {
        collection.collectAll();
        NewsSource broken = sources.findById("broken").orElseThrow();
        NewsSource missing = sources.findById("missing").orElseThrow();
        NewsSource good = sources.findById("good").orElseThrow();
        assertThat(broken.getLastStatus()).isEqualTo(NewsSource.Status.ERROR);
        assertThat(broken.getLastError()).contains("Invalid XML");
        assertThat(missing.getLastStatus()).isEqualTo(NewsSource.Status.ERROR);
        assertThat(missing.getLastError()).contains("404");
        assertThat(good.getLastStatus()).isEqualTo(NewsSource.Status.OK);
        assertThat(good.getLastNewItems()).isEqualTo(2);
        assertThat(good.getLastSuccessAt()).isNotNull();

        collection.collectAll();
        assertThat(sources.findById("missing").orElseThrow().getConsecutiveFailures()).isEqualTo(2);
    }

    @Test
    void disabledSourcesAreSkipped() {
        sources.findById("good").ifPresent(s -> { s.setEnabled(false); sources.save(s); });
        assertThat(collection.collectAll().added()).isZero();
    }

    @Test
    void reloadingSampleDataNeverDeletesCollectedNews() {
        collection.collectAll();
        ingestion.reload();
        assertThat(items.countByProvenanceDataStatus(DataStatus.LIVE)).isEqualTo(2);
        assertThat(items.countByProvenanceDataStatus(DataStatus.SAMPLE)).isPositive();
        assertThat(items.findAll()).filteredOn(n -> !n.isLive()).allSatisfy(n -> {
            assertThat(n.getProvenance().getSourceUrl()).isNull();
            assertThat(n.getTitle()).startsWith("Sample:");
        });
    }

    @Test
    void registryRejectsInvalidAndPrivateUrls(@Autowired NewsSourceRegistry registry) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registry.register("x", "ftp://example.com/feed", null, null, null, null))
            .isInstanceOf(FeedException.class).hasMessageContaining("http");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registry.register("x", "http://127.0.0.1/feed", null, null, null, null))
            .isInstanceOf(FeedException.class).hasMessageContaining("Private");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registry.register("", "https://example.com/feed", null, null, null, null))
            .isInstanceOf(FeedException.class);
    }
}
