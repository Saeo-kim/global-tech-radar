package com.globaltechradar;

import com.globaltechradar.domain.*;
import com.globaltechradar.provider.SampleIntelligenceDataProvider;
import com.globaltechradar.service.IntelligenceService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards the data rules: required coverage, and every record labelled as sample with source + date. */
class SampleDataProviderTest {

    private final SampleIntelligenceDataProvider provider = new SampleIntelligenceDataProvider();

    @Test
    void coversRequiredCountries() {
        assertThat(provider.countries()).extracting(Country::getCode)
            .containsExactlyInAnyOrder("US", "CN", "JP", "KR", "IN", "DE", "GB", "FR", "SG");
    }

    @Test
    void everyRadarDomainHasTechnologies() {
        Set<String> domains = provider.technologies().stream().map(Technology::getDomain).collect(Collectors.toSet());
        assertThat(domains).containsExactlyInAnyOrderElementsOf(IntelligenceService.DOMAINS.keySet());
        assertThat(provider.technologies()).extracting(Technology::getSlug).doesNotHaveDuplicates();
    }

    @Test
    void coversRequiredCompanies() {
        assertThat(provider.companies()).extracting(Company::getName).containsExactlyInAnyOrder(
            "Google", "Microsoft", "Amazon", "Apple", "NVIDIA", "OpenAI", "Meta", "Samsung", "Alibaba", "Tencent", "TSMC");
    }

    @Test
    void coversAllWorkMarketCategories() {
        Set<MarketCategory> categories = provider.marketSignals().stream().map(MarketSignal::getCategory).collect(Collectors.toSet());
        assertThat(categories).containsExactlyInAnyOrder(MarketCategory.values());
    }

    @Test
    void everyRecordIsLabelledSampleWithSourceAndDate() {
        List<DataProvenance> all = Stream.of(
            provider.countries().stream().map(Country::getProvenance),
            provider.technologies().stream().map(Technology::getProvenance),
            provider.companies().stream().map(Company::getProvenance),
            provider.marketSignals().stream().map(MarketSignal::getProvenance),
            provider.trendPoints().stream().map(TrendPoint::getProvenance),
            provider.reports().stream().map(IntelligenceReport::getProvenance)
        ).flatMap(s -> s).toList();

        assertThat(all).isNotEmpty().allSatisfy(p -> {
            assertThat(p.getDataStatus()).isEqualTo(DataStatus.SAMPLE);
            assertThat(p.getSourceName()).isNotBlank();
            assertThat(p.getPublishedOn()).isNotNull();
        });
    }

    @Test
    void everyInDemandSkillHasALearningPath() {
        Set<String> paths = provider.learningPaths().stream().map(LearningPath::getSkillKey).collect(Collectors.toSet());
        provider.marketSignals().stream().filter(m -> m.getCategory() == MarketCategory.SKILL_DEMAND)
            .forEach(m -> assertThat(paths).as("learning path for " + m.getTitle()).contains(LearningPath.normalise(m.getTitle())));
    }
}
