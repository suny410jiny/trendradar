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
}
