package com.trendradar.scheduler;

import com.trendradar.channel.service.ChannelCollectService;
import com.trendradar.keyword.service.KeywordTrendService;
import com.trendradar.tag.service.AlgorithmTagService;
import com.trendradar.trending.domain.TrendingVideo;
import com.trendradar.trending.service.TrendingCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrendingScheduler {

    private static final List<String> TARGET_COUNTRIES = List.of("KR", "US", "JP", "GB", "DE");

    private final TrendingCollectService trendingCollectService;
    private final AlgorithmTagService algorithmTagService;
    private final ChannelCollectService channelCollectService;
    private final KeywordTrendService keywordTrendService;

    @Scheduled(cron = "0 0 * * * *")
    public void collectAllCountries() {
        log.info("Starting trending collection for {} countries", TARGET_COUNTRIES.size());
        long startTime = System.currentTimeMillis();

        // Step 1: 5개국 비동기 병렬 수집
        List<CompletableFuture<List<TrendingVideo>>> futures = TARGET_COUNTRIES.stream()
                .map(trendingCollectService::collectAsync)
                .toList();

        List<TrendingVideo> allVideos = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                List<TrendingVideo> result = futures.get(i).join();
                allVideos.addAll(result);
            } catch (Exception e) {
                log.error("Failed to collect trending for country={}", TARGET_COUNTRIES.get(i), e);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Collected {} videos from {} countries in {}ms",
                allVideos.size(), TARGET_COUNTRIES.size(), elapsed);

        if (allVideos.isEmpty()) return;

        OffsetDateTime collectedAt = OffsetDateTime.now(ZoneOffset.UTC);

        // Step 2: 알고리즘 태그 계산
        try {
            algorithmTagService.calculateTags(allVideos, collectedAt);
            log.info("Algorithm tag calculation completed");
        } catch (Exception e) {
            log.error("Failed to calculate algorithm tags", e);
        }

        // Step 3: 채널 정보 수집
        try {
            List<String> channelIds = allVideos.stream()
                    .map(TrendingVideo::getChannelId)
                    .filter(id -> id != null && !id.isBlank())
                    .distinct()
                    .toList();
            channelCollectService.collectChannels(channelIds, collectedAt);
            log.info("Channel collection completed for {} channels", channelIds.size());
        } catch (Exception e) {
            log.error("Failed to collect channel data", e);
        }

        // Step 4: 키워드 트렌드 집계 (나라별)
        try {
            Map<String, List<TrendingVideo>> byCountry = allVideos.stream()
                    .collect(Collectors.groupingBy(TrendingVideo::getCountryCode));
            for (Map.Entry<String, List<TrendingVideo>> entry : byCountry.entrySet()) {
                keywordTrendService.aggregateHourlyKeywords(entry.getValue(), entry.getKey(), collectedAt);
            }
            log.info("Keyword aggregation completed for {} countries", byCountry.size());
        } catch (Exception e) {
            log.error("Failed to aggregate keywords", e);
        }
    }
}
