package com.trendradar.scheduler;

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
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrendingScheduler {

    private static final List<String> TARGET_COUNTRIES = List.of("KR", "US", "JP", "GB", "DE");

    private final TrendingCollectService trendingCollectService;
    private final AlgorithmTagService algorithmTagService;

    @Scheduled(cron = "0 0 * * * *")
    public void collectAllCountries() {
        log.info("Starting trending collection for {} countries", TARGET_COUNTRIES.size());
        long startTime = System.currentTimeMillis();

        // 5개국 비동기 병렬 수집
        List<CompletableFuture<List<TrendingVideo>>> futures = TARGET_COUNTRIES.stream()
                .map(trendingCollectService::collectAsync)
                .toList();

        // 전체 완료 대기 + 결과 수집
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

        // 수집 완료 후 알고리즘 태그 계산
        if (!allVideos.isEmpty()) {
            try {
                OffsetDateTime collectedAt = OffsetDateTime.now(ZoneOffset.UTC);
                algorithmTagService.calculateTags(allVideos, collectedAt);
                log.info("Algorithm tag calculation completed");
            } catch (Exception e) {
                log.error("Failed to calculate algorithm tags", e);
            }
        }
    }
}
