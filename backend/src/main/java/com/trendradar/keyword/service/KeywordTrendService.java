package com.trendradar.keyword.service;

import com.trendradar.keyword.domain.KeywordTrend;
import com.trendradar.keyword.domain.PeriodType;
import com.trendradar.keyword.repository.KeywordTrendRepository;
import com.trendradar.trending.domain.TrendingVideo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class KeywordTrendService {

    private final KeywordTrendRepository keywordTrendRepository;

    @Transactional
    public void aggregateHourlyKeywords(List<TrendingVideo> videos, String countryCode,
                                        OffsetDateTime collectedAt) {
        if (videos == null || videos.isEmpty()) {
            log.info("No videos to aggregate keywords from, skipping");
            return;
        }

        OffsetDateTime periodStart = collectedAt.truncatedTo(ChronoUnit.HOURS);

        // 1. Extract tags from all videos and group by normalized keyword
        Map<String, List<TrendingVideo>> keywordToVideos = new HashMap<>();
        for (TrendingVideo video : videos) {
            List<String> tags = video.getYoutubeTagList();
            for (String tag : tags) {
                String normalized = tag.trim().toLowerCase();
                if (!normalized.isEmpty()) {
                    keywordToVideos.computeIfAbsent(normalized, k -> new ArrayList<>()).add(video);
                }
            }
        }

        if (keywordToVideos.isEmpty()) {
            log.info("No keywords extracted from {} videos, skipping", videos.size());
            return;
        }

        log.info("Aggregating {} unique keywords from {} videos for country={}",
                keywordToVideos.size(), videos.size(), countryCode);

        // 2. For each keyword, calculate stats and upsert
        for (Map.Entry<String, List<TrendingVideo>> entry : keywordToVideos.entrySet()) {
            String keyword = entry.getKey();
            List<TrendingVideo> keywordVideos = entry.getValue();

            int videoCount = keywordVideos.size();
            long totalViews = keywordVideos.stream()
                    .mapToLong(v -> v.getViewCount() != null ? v.getViewCount() : 0L)
                    .sum();
            double avgEngagement = keywordVideos.stream()
                    .mapToDouble(v -> {
                        long views = v.getViewCount() != null ? v.getViewCount() : 0L;
                        long likes = v.getLikeCount() != null ? v.getLikeCount() : 0L;
                        return views > 0 ? (double) likes / views : 0.0;
                    })
                    .average()
                    .orElse(0.0);

            // UPSERT: find existing or create new
            Optional<KeywordTrend> existing = keywordTrendRepository
                    .findByKeywordAndCountryCodeAndPeriodTypeAndPeriodStart(
                            keyword, countryCode, PeriodType.HOUR, periodStart);

            if (existing.isPresent()) {
                existing.get().updateStats(videoCount, totalViews, avgEngagement);
                log.debug("Updated keyword trend: keyword={}, videoCount={}", keyword, videoCount);
            } else {
                KeywordTrend newTrend = KeywordTrend.builder()
                        .keyword(keyword)
                        .countryCode(countryCode)
                        .videoCount(videoCount)
                        .totalViews(totalViews)
                        .avgEngagement(avgEngagement)
                        .periodType(PeriodType.HOUR)
                        .periodStart(periodStart)
                        .build();
                keywordTrendRepository.save(newTrend);
                log.debug("Created keyword trend: keyword={}, videoCount={}", keyword, videoCount);
            }
        }

        log.info("Keyword aggregation completed: {} keywords for country={}",
                keywordToVideos.size(), countryCode);
    }

    /**
     * 일별 집계: 해당 날짜의 모든 HOUR 레코드를 keyword별로 합산하여 DAY 레코드 UPSERT
     */
    @Transactional
    public void aggregateDailyKeywords(String countryCode, OffsetDateTime date) {
        OffsetDateTime dayStart = date.truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime dayEnd = dayStart.plusDays(1);

        List<KeywordTrend> hourlyRecords = keywordTrendRepository
                .findByCountryCodeAndPeriodTypeAndPeriodStartBetween(
                        countryCode, PeriodType.HOUR, dayStart, dayEnd);

        if (hourlyRecords.isEmpty()) {
            log.info("No hourly records found for daily aggregation: country={}, date={}",
                    countryCode, dayStart);
            return;
        }

        aggregateAndUpsert(hourlyRecords, countryCode, PeriodType.DAY, dayStart);

        log.info("Daily keyword aggregation completed: country={}, date={}", countryCode, dayStart);
    }

    /**
     * 주별 집계: weekStart부터 7일간의 모든 DAY 레코드를 keyword별로 합산하여 WEEK 레코드 UPSERT
     */
    @Transactional
    public void aggregateWeeklyKeywords(String countryCode, OffsetDateTime weekStart) {
        OffsetDateTime start = weekStart.truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime end = start.plusWeeks(1);

        List<KeywordTrend> dailyRecords = keywordTrendRepository
                .findByCountryCodeAndPeriodTypeAndPeriodStartBetween(
                        countryCode, PeriodType.DAY, start, end);

        if (dailyRecords.isEmpty()) {
            log.info("No daily records found for weekly aggregation: country={}, weekStart={}",
                    countryCode, start);
            return;
        }

        aggregateAndUpsert(dailyRecords, countryCode, PeriodType.WEEK, start);

        log.info("Weekly keyword aggregation completed: country={}, weekStart={}", countryCode, start);
    }

    /**
     * 월별 집계: monthStart부터 해당 월의 모든 DAY 레코드를 keyword별로 합산하여 MONTH 레코드 UPSERT
     */
    @Transactional
    public void aggregateMonthlyKeywords(String countryCode, OffsetDateTime monthStart) {
        OffsetDateTime start = monthStart.truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime end = start.plusMonths(1);

        List<KeywordTrend> dailyRecords = keywordTrendRepository
                .findByCountryCodeAndPeriodTypeAndPeriodStartBetween(
                        countryCode, PeriodType.DAY, start, end);

        if (dailyRecords.isEmpty()) {
            log.info("No daily records found for monthly aggregation: country={}, monthStart={}",
                    countryCode, start);
            return;
        }

        aggregateAndUpsert(dailyRecords, countryCode, PeriodType.MONTH, start);

        log.info("Monthly keyword aggregation completed: country={}, monthStart={}", countryCode, start);
    }

    /**
     * 공통 집계 로직: 소스 레코드들을 keyword별로 그룹핑 → 합산/평균 → UPSERT
     */
    private void aggregateAndUpsert(List<KeywordTrend> sourceRecords, String countryCode,
                                     PeriodType targetPeriodType, OffsetDateTime periodStart) {
        Map<String, List<KeywordTrend>> byKeyword = sourceRecords.stream()
                .collect(Collectors.groupingBy(KeywordTrend::getKeyword));

        for (Map.Entry<String, List<KeywordTrend>> entry : byKeyword.entrySet()) {
            String keyword = entry.getKey();
            List<KeywordTrend> records = entry.getValue();

            int totalVideoCount = records.stream()
                    .mapToInt(KeywordTrend::getVideoCount)
                    .sum();
            long totalViews = records.stream()
                    .mapToLong(KeywordTrend::getTotalViews)
                    .sum();
            double avgEngagement = records.stream()
                    .mapToDouble(KeywordTrend::getAvgEngagement)
                    .average()
                    .orElse(0.0);

            Optional<KeywordTrend> existing = keywordTrendRepository
                    .findByKeywordAndCountryCodeAndPeriodTypeAndPeriodStart(
                            keyword, countryCode, targetPeriodType, periodStart);

            if (existing.isPresent()) {
                existing.get().updateStats(totalVideoCount, totalViews, avgEngagement);
            } else {
                KeywordTrend newTrend = KeywordTrend.builder()
                        .keyword(keyword)
                        .countryCode(countryCode)
                        .videoCount(totalVideoCount)
                        .totalViews(totalViews)
                        .avgEngagement(avgEngagement)
                        .periodType(targetPeriodType)
                        .periodStart(periodStart)
                        .build();
                keywordTrendRepository.save(newTrend);
            }
        }
    }
}
