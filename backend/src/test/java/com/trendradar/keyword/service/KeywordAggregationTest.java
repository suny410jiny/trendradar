package com.trendradar.keyword.service;

import com.trendradar.keyword.domain.KeywordTrend;
import com.trendradar.keyword.domain.PeriodType;
import com.trendradar.keyword.repository.KeywordTrendRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("키워드 배치 집계 테스트")
@ExtendWith(MockitoExtension.class)
class KeywordAggregationTest {

    @Mock
    private KeywordTrendRepository keywordTrendRepository;

    @InjectMocks
    private KeywordTrendService keywordTrendService;

    private static final OffsetDateTime BASE_DATE =
            OffsetDateTime.of(2026, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Test
    @DisplayName("aggregateDailyKeywords - 3개 HOUR 레코드를 합산하여 1개 DAY 레코드 생성")
    void aggregateDailyKeywords_sumsHourlyRecords() {
        // Given - 3 HOUR records for the same keyword "먹방" on the same day
        KeywordTrend hour1 = KeywordTrend.builder()
                .keyword("먹방")
                .countryCode("KR")
                .videoCount(5)
                .totalViews(100_000L)
                .avgEngagement(0.05)
                .periodType(PeriodType.HOUR)
                .periodStart(BASE_DATE)
                .build();

        KeywordTrend hour2 = KeywordTrend.builder()
                .keyword("먹방")
                .countryCode("KR")
                .videoCount(3)
                .totalViews(80_000L)
                .avgEngagement(0.04)
                .periodType(PeriodType.HOUR)
                .periodStart(BASE_DATE.plusHours(1))
                .build();

        KeywordTrend hour3 = KeywordTrend.builder()
                .keyword("먹방")
                .countryCode("KR")
                .videoCount(7)
                .totalViews(120_000L)
                .avgEngagement(0.06)
                .periodType(PeriodType.HOUR)
                .periodStart(BASE_DATE.plusHours(2))
                .build();

        given(keywordTrendRepository.findByCountryCodeAndPeriodTypeAndPeriodStartBetween(
                eq("KR"), eq(PeriodType.HOUR), eq(BASE_DATE), eq(BASE_DATE.plusDays(1))))
                .willReturn(List.of(hour1, hour2, hour3));

        // No existing DAY record
        given(keywordTrendRepository.findByKeywordAndCountryCodeAndPeriodTypeAndPeriodStart(
                eq("먹방"), eq("KR"), eq(PeriodType.DAY), eq(BASE_DATE)))
                .willReturn(Optional.empty());

        given(keywordTrendRepository.save(any(KeywordTrend.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        keywordTrendService.aggregateDailyKeywords("KR", BASE_DATE);

        // Then - 1 DAY record created with summed values
        ArgumentCaptor<KeywordTrend> captor = ArgumentCaptor.forClass(KeywordTrend.class);
        verify(keywordTrendRepository, times(1)).save(captor.capture());

        KeywordTrend saved = captor.getValue();
        assertThat(saved.getKeyword()).isEqualTo("먹방");
        assertThat(saved.getCountryCode()).isEqualTo("KR");
        assertThat(saved.getPeriodType()).isEqualTo(PeriodType.DAY);
        assertThat(saved.getPeriodStart()).isEqualTo(BASE_DATE);
        assertThat(saved.getVideoCount()).isEqualTo(15);  // 5 + 3 + 7
        assertThat(saved.getTotalViews()).isEqualTo(300_000L);  // 100K + 80K + 120K
        assertThat(saved.getAvgEngagement()).isCloseTo(0.05, within(0.001));  // avg(0.05, 0.04, 0.06)
    }

    @Test
    @DisplayName("aggregateWeeklyKeywords - 3개 DAY 레코드를 합산하여 1개 WEEK 레코드 생성")
    void aggregateWeeklyKeywords_sumsDailyRecords() {
        // Given - 3 DAY records for the same keyword "게임" across 3 days
        OffsetDateTime weekStart = BASE_DATE; // Monday

        KeywordTrend day1 = KeywordTrend.builder()
                .keyword("게임")
                .countryCode("US")
                .videoCount(10)
                .totalViews(500_000L)
                .avgEngagement(0.03)
                .periodType(PeriodType.DAY)
                .periodStart(weekStart)
                .build();

        KeywordTrend day2 = KeywordTrend.builder()
                .keyword("게임")
                .countryCode("US")
                .videoCount(8)
                .totalViews(400_000L)
                .avgEngagement(0.04)
                .periodType(PeriodType.DAY)
                .periodStart(weekStart.plusDays(1))
                .build();

        KeywordTrend day3 = KeywordTrend.builder()
                .keyword("게임")
                .countryCode("US")
                .videoCount(12)
                .totalViews(600_000L)
                .avgEngagement(0.05)
                .periodType(PeriodType.DAY)
                .periodStart(weekStart.plusDays(2))
                .build();

        given(keywordTrendRepository.findByCountryCodeAndPeriodTypeAndPeriodStartBetween(
                eq("US"), eq(PeriodType.DAY), eq(weekStart), eq(weekStart.plusWeeks(1))))
                .willReturn(List.of(day1, day2, day3));

        // No existing WEEK record
        given(keywordTrendRepository.findByKeywordAndCountryCodeAndPeriodTypeAndPeriodStart(
                eq("게임"), eq("US"), eq(PeriodType.WEEK), eq(weekStart)))
                .willReturn(Optional.empty());

        given(keywordTrendRepository.save(any(KeywordTrend.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        keywordTrendService.aggregateWeeklyKeywords("US", weekStart);

        // Then - 1 WEEK record created with summed values
        ArgumentCaptor<KeywordTrend> captor = ArgumentCaptor.forClass(KeywordTrend.class);
        verify(keywordTrendRepository, times(1)).save(captor.capture());

        KeywordTrend saved = captor.getValue();
        assertThat(saved.getKeyword()).isEqualTo("게임");
        assertThat(saved.getCountryCode()).isEqualTo("US");
        assertThat(saved.getPeriodType()).isEqualTo(PeriodType.WEEK);
        assertThat(saved.getPeriodStart()).isEqualTo(weekStart);
        assertThat(saved.getVideoCount()).isEqualTo(30);  // 10 + 8 + 12
        assertThat(saved.getTotalViews()).isEqualTo(1_500_000L);  // 500K + 400K + 600K
        assertThat(saved.getAvgEngagement()).isCloseTo(0.04, within(0.001));  // avg(0.03, 0.04, 0.05)
    }
}
