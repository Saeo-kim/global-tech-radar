package com.globaltechradar.collector;

import com.globaltechradar.domain.NewsCategory;
import com.globaltechradar.domain.NewsSourceType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NewsClassifierTest {

    private final NewsClassifier c = new NewsClassifier();

    @Test
    void classifiesByKeywordsWithFeedDefaultAsTieBreaker() {
        assertThat(c.category("Humanoid robot learns warehouse picking", "A robotics team showed...", NewsCategory.AI))
            .isEqualTo(NewsCategory.ROBOTICS);
        assertThat(c.category("Critical vulnerability exploited in VPN appliances", "Patch now.", NewsCategory.CLOUD_DEVOPS))
            .isEqualTo(NewsCategory.CYBERSECURITY);
        assertThat(c.category("Weekly roundup", "", NewsCategory.SPACE)).isEqualTo(NewsCategory.SPACE);
        assertThat(c.category("New AI agents automate coding tasks", "", NewsCategory.AI)).isEqualTo(NewsCategory.AI_AGENTS);
        // Regression (real NASA feed item): a tie between headline keywords and the feed default goes to the default.
        assertThat(c.category("Curiosity Blog, Sols 5063-5069: Rocks and Chips", "Space Science Institute drill campaign", NewsCategory.SPACE))
            .isEqualTo(NewsCategory.SPACE);
    }

    @Test
    void detectsRegionAndFallsBackToFeedDefault() {
        assertThat(c.region("Seoul startup ships on-device model", "", "US")).isEqualTo("KR");
        assertThat(c.region("European regulators publish guidance", "", "US")).isEqualTo("Europe");
        assertThat(c.region("A new compiler release", "", "Global")).isEqualTo("Global");
        assertThat(c.region("Unrelated", null, null)).isEqualTo("Global");
    }

    @Test
    void detectsCompaniesAsWholeWordsAndCanonicalisesAliases() {
        assertThat(c.companies("Nvidia and TSMC expand packaging", "with Samsung", null))
            .containsExactly("NVIDIA", "Samsung", "TSMC");
        assertThat(c.companies("Metadata pipelines", "metaverse", null)).doesNotContain("Meta");
        assertThat(c.companies("Something", "", "OpenAI")).containsExactly("OpenAI");
    }

    @Test
    void importanceIsBoundedAndRewardsSignals() {
        int low = c.importance("Notes", null, NewsSourceType.MEDIA, NewsCategory.EMERGING, 0);
        int high = c.importance("NVIDIA announces and launches a breakthrough accelerator",
            "A long summary that is clearly more than one hundred and twenty characters long, describing the release in detail for readers.",
            NewsSourceType.GOVERNMENT, NewsCategory.SEMICONDUCTOR, 4);
        assertThat(low).isBetween(0, 100);
        assertThat(high).isBetween(0, 100).isGreaterThan(low);
    }

    @Test
    void normalisesUrlsForDeduplication() {
        assertThat(NewsClassifier.normaliseUrl("http://WWW.Example.com/post/?utm_source=rss&id=3#top"))
            .isEqualTo("https://example.com/post?id=3");
        assertThat(NewsClassifier.normaliseUrl("https://example.com/post"))
            .isEqualTo(NewsClassifier.normaliseUrl("https://example.com/post/?utm_campaign=x"));
        assertThat(NewsClassifier.titleKey("  OpenAI: New Model — Released!  ")).isEqualTo("openai new model released");
        assertThat(NewsClassifier.sha256("a")).hasSize(64);
    }
}
