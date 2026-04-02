package com.trendradar.keyword.controller;

import com.trendradar.common.response.ApiResponse;
import com.trendradar.keyword.domain.KeywordTrend;
import com.trendradar.keyword.domain.PeriodType;
import com.trendradar.keyword.dto.KeywordTimelineResponse;
import com.trendradar.keyword.dto.KeywordTrendResponse;
import com.trendradar.keyword.repository.KeywordTrendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v2/keywords")
@RequiredArgsConstructor
public class KeywordController {

    private final KeywordTrendRepository keywordTrendRepository;

    @GetMapping("/trending")
    public ApiResponse<List<KeywordTrendResponse>> getTrending(
            @RequestParam String country,
            @RequestParam(defaultValue = "DAY") String period,
            @RequestParam(defaultValue = "50") int limit) {
        PeriodType periodType = PeriodType.valueOf(period.toUpperCase());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime since = switch (periodType) {
            case HOUR -> now.minusHours(2);
            case DAY -> now.minusDays(1);
            case WEEK -> now.minusWeeks(1);
            case MONTH -> now.minusMonths(1);
            case QUARTER -> now.minusMonths(3);
            case YEAR -> now.minusYears(1);
        };

        List<KeywordTrend> trends = keywordTrendRepository
                .findByCountryCodeAndPeriodTypeAndPeriodStartAfter(country, periodType, since);

        // Sort by videoCount DESC, limit, convert to response DTO
        // keywordScore is a placeholder (0) for now — full scoring formula will come later
        List<KeywordTrendResponse> responses = trends.stream()
                .sorted((a, b) -> Integer.compare(b.getVideoCount(), a.getVideoCount()))
                .limit(limit)
                .map(kt -> KeywordTrendResponse.builder()
                        .keyword(kt.getKeyword())
                        .videoCount(kt.getVideoCount())
                        .totalViews(kt.getTotalViews())
                        .avgEngagement(kt.getAvgEngagement())
                        .keywordScore(0) // placeholder
                        .build())
                .toList();

        return ApiResponse.of(responses);
    }

    @GetMapping("/{keyword}/timeline")
    public ApiResponse<List<KeywordTimelineResponse>> getTimeline(
            @PathVariable String keyword,
            @RequestParam(defaultValue = "WEEK") String period) {
        PeriodType periodType = PeriodType.valueOf(period.toUpperCase());
        List<KeywordTrend> trends = keywordTrendRepository
                .findByKeywordAndPeriodTypeOrderByPeriodStartDesc(keyword.toLowerCase(), periodType);

        List<KeywordTimelineResponse> responses = trends.stream()
                .map(kt -> KeywordTimelineResponse.builder()
                        .keyword(kt.getKeyword())
                        .periodType(kt.getPeriodType().name())
                        .periodStart(kt.getPeriodStart())
                        .videoCount(kt.getVideoCount())
                        .totalViews(kt.getTotalViews())
                        .build())
                .toList();

        return ApiResponse.of(responses);
    }
}
