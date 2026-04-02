package com.trendradar.scheduler;

import com.trendradar.ai.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisScheduler {

    private static final List<String> TARGET_COUNTRIES = List.of("KR", "US", "JP", "GB", "DE");
    private final AiAnalysisService aiAnalysisService;

    @Scheduled(cron = "0 30 * * * *")  // Every hour at :30
    public void generateBriefings() {
        log.info("Starting AI briefing generation for {} countries", TARGET_COUNTRIES.size());
        for (String country : TARGET_COUNTRIES) {
            try {
                aiAnalysisService.generateBriefing(country);
                log.info("Generated briefing for country={}", country);
            } catch (Exception e) {
                log.error("Failed to generate briefing for country={}", country, e);
            }
        }
    }

    @Scheduled(cron = "0 45 */6 * * *")  // Every 6 hours at :45
    public void generatePredictions() {
        log.info("Starting AI prediction generation for {} countries", TARGET_COUNTRIES.size());
        for (String country : TARGET_COUNTRIES) {
            try {
                aiAnalysisService.generatePrediction(country);
                log.info("Generated prediction for country={}", country);
            } catch (Exception e) {
                log.error("Failed to generate prediction for country={}", country, e);
            }
        }
    }
}
