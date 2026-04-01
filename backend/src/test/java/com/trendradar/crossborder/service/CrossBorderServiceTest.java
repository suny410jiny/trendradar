package com.trendradar.crossborder.service;

import com.trendradar.crossborder.dto.GlobalLocalResponse;
import com.trendradar.crossborder.dto.OpportunityResponse;
import com.trendradar.crossborder.dto.PropagationResponse;
import com.trendradar.keyword.domain.KeywordTrend;
import com.trendradar.keyword.domain.PeriodType;
import com.trendradar.keyword.repository.KeywordTrendRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CrossBorderServiceTest {

    @Mock
    private KeywordTrendRepository keywordTrendRepository;

    @InjectMocks
    private CrossBorderService crossBorderService;

    // -- helpers --

    private KeywordTrend buildTrend(String keyword, String countryCode,
                                     int videoCount, long totalViews,
                                     PeriodType periodType, OffsetDateTime periodStart) {
        return KeywordTrend.builder()
                .keyword(keyword)
                .countryCode(countryCode)
                .videoCount(videoCount)
                .totalViews(totalViews)
                .avgEngagement(0.05)
                .periodType(periodType)
                .periodStart(periodStart)
                .build();
    }

    // ==================== View 1: Opportunities ====================

    @Test
    void findOpportunities_returnsKeywordsNotInMyCountry() {
        // Given: "challenge" is trending in US and JP, but NOT in KR
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.HOURS);

        List<KeywordTrend> allTrends = List.of(
                buildTrend("challenge", "US", 5, 1_000_000L, PeriodType.HOUR, now),
                buildTrend("challenge", "JP", 3, 500_000L, PeriodType.HOUR, now),
                buildTrend("music", "KR", 10, 2_000_000L, PeriodType.HOUR, now),
                buildTrend("music", "US", 8, 1_500_000L, PeriodType.HOUR, now)
        );

        given(keywordTrendRepository.findByPeriodTypeAndPeriodStartAfter(
                eq(PeriodType.HOUR), any(OffsetDateTime.class)))
                .willReturn(allTrends);

        // When
        List<OpportunityResponse> result = crossBorderService.findOpportunities("KR");

        // Then: "challenge" should appear (not in KR), "music" should NOT (already in KR)
        assertThat(result).hasSize(1);

        OpportunityResponse opportunity = result.get(0);
        assertThat(opportunity.getKeyword()).isEqualTo("challenge");
        assertThat(opportunity.getTrendingCountries()).containsExactlyInAnyOrder("US", "JP");
        assertThat(opportunity.getTargetCountry()).isEqualTo("KR");
        assertThat(opportunity.getTotalVideoCount()).isEqualTo(8);
        assertThat(opportunity.getTotalViews()).isEqualTo(1_500_000L);
    }

    // ==================== View 2: Propagation ====================

    @Test
    void findPropagation_returnsCountriesSortedByFirstSeen() {
        // Given: "viral" appeared US(3/28) -> GB(3/29) -> KR(3/30)
        OffsetDateTime march28 = OffsetDateTime.parse("2026-03-28T00:00:00Z");
        OffsetDateTime march29 = OffsetDateTime.parse("2026-03-29T00:00:00Z");
        OffsetDateTime march30 = OffsetDateTime.parse("2026-03-30T00:00:00Z");

        List<KeywordTrend> trends = List.of(
                buildTrend("viral", "US", 5, 1_000_000L, PeriodType.HOUR, march28),
                buildTrend("viral", "US", 7, 2_000_000L, PeriodType.HOUR, march29),
                buildTrend("viral", "GB", 3, 500_000L, PeriodType.HOUR, march29),
                buildTrend("viral", "KR", 2, 300_000L, PeriodType.HOUR, march30)
        );

        given(keywordTrendRepository.findByKeywordAndPeriodTypeOrderByPeriodStartDesc(
                eq("viral"), eq(PeriodType.HOUR)))
                .willReturn(trends);

        // When
        PropagationResponse result = crossBorderService.findPropagation("viral");

        // Then: path should be US -> GB -> KR in chronological order
        assertThat(result.getKeyword()).isEqualTo("viral");
        assertThat(result.getPropagationPath()).hasSize(3);

        List<String> countryCodes = result.getPropagationPath().stream()
                .map(PropagationResponse.PropagationStep::getCountryCode)
                .toList();
        assertThat(countryCodes).containsExactly("US", "GB", "KR");

        assertThat(result.getPropagationPath().get(0).getFirstSeenAt()).isEqualTo(march28);
        assertThat(result.getPropagationPath().get(1).getFirstSeenAt()).isEqualTo(march29);
        assertThat(result.getPropagationPath().get(2).getFirstSeenAt()).isEqualTo(march30);
    }

    // ==================== View 3: Global vs Local ====================

    @Test
    void findGlobalVsLocal_classifiesCorrectly() {
        // Given:
        // "music" in KR, US, JP, GB -> global (4 countries >= 3)
        // "국회의원" in KR only -> local for KR
        // "baseball" in US only -> NOT KR's local
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.HOURS);

        List<KeywordTrend> allTrends = List.of(
                buildTrend("music", "KR", 10, 2_000_000L, PeriodType.HOUR, now),
                buildTrend("music", "US", 8, 1_500_000L, PeriodType.HOUR, now),
                buildTrend("music", "JP", 6, 1_000_000L, PeriodType.HOUR, now),
                buildTrend("music", "GB", 4, 800_000L, PeriodType.HOUR, now),
                buildTrend("국회의원", "KR", 3, 500_000L, PeriodType.HOUR, now),
                buildTrend("baseball", "US", 5, 700_000L, PeriodType.HOUR, now)
        );

        given(keywordTrendRepository.findByPeriodTypeAndPeriodStartAfter(
                eq(PeriodType.HOUR), any(OffsetDateTime.class)))
                .willReturn(allTrends);

        // When
        GlobalLocalResponse result = crossBorderService.findGlobalVsLocal("KR");

        // Then
        // Global: "music" (4 countries >= 3)
        assertThat(result.getGlobalKeywords()).hasSize(1);
        assertThat(result.getGlobalKeywords().get(0).getKeyword()).isEqualTo("music");
        assertThat(result.getGlobalKeywords().get(0).getCountries())
                .containsExactlyInAnyOrder("KR", "US", "JP", "GB");
        assertThat(result.getGlobalKeywords().get(0).getVideoCount()).isEqualTo(28);

        // Local: only "국회의원" (KR only), NOT "baseball" (US only, not KR)
        assertThat(result.getLocalKeywords()).hasSize(1);
        assertThat(result.getLocalKeywords().get(0).getKeyword()).isEqualTo("국회의원");
        assertThat(result.getLocalKeywords().get(0).getCountries()).containsExactly("KR");

        // "baseball" should NOT be in local keywords (it's US only, not KR)
        List<String> localKeywordNames = result.getLocalKeywords().stream()
                .map(GlobalLocalResponse.KeywordInfo::getKeyword)
                .toList();
        assertThat(localKeywordNames).doesNotContain("baseball");
    }
}
