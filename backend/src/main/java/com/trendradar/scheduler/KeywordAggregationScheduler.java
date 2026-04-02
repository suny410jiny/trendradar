package com.trendradar.scheduler;

import com.trendradar.keyword.service.KeywordTrendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeywordAggregationScheduler {

    private static final List<String> TARGET_COUNTRIES = List.of("KR", "US", "JP", "GB", "DE");
    private final KeywordTrendService keywordTrendService;

    @Scheduled(cron = "0 5 0 * * *")  // Daily at 00:05
    public void aggregateDaily() {
        OffsetDateTime yesterday = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        log.info("Starting daily keyword aggregation for {} countries", TARGET_COUNTRIES.size());
        for (String country : TARGET_COUNTRIES) {
            try {
                keywordTrendService.aggregateDailyKeywords(country, yesterday);
                log.info("Daily keyword aggregation completed for country={}", country);
            } catch (Exception e) {
                log.error("Failed daily keyword aggregation for country={}", country, e);
            }
        }
    }

    @Scheduled(cron = "0 10 0 * * MON")  // Weekly on Monday at 00:10
    public void aggregateWeekly() {
        OffsetDateTime lastWeekStart = OffsetDateTime.now(ZoneOffset.UTC).minusWeeks(1)
                .with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
        log.info("Starting weekly keyword aggregation for {} countries", TARGET_COUNTRIES.size());
        for (String country : TARGET_COUNTRIES) {
            try {
                keywordTrendService.aggregateWeeklyKeywords(country, lastWeekStart);
                log.info("Weekly keyword aggregation completed for country={}", country);
            } catch (Exception e) {
                log.error("Failed weekly keyword aggregation for country={}", country, e);
            }
        }
    }

    @Scheduled(cron = "0 15 0 1 * *")  // Monthly on 1st at 00:15
    public void aggregateMonthly() {
        OffsetDateTime lastMonthStart = OffsetDateTime.now(ZoneOffset.UTC).minusMonths(1)
                .withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        log.info("Starting monthly keyword aggregation for {} countries", TARGET_COUNTRIES.size());
        for (String country : TARGET_COUNTRIES) {
            try {
                keywordTrendService.aggregateMonthlyKeywords(country, lastMonthStart);
                log.info("Monthly keyword aggregation completed for country={}", country);
            } catch (Exception e) {
                log.error("Failed monthly keyword aggregation for country={}", country, e);
            }
        }
    }
}
