package com.trendradar.crossborder.service;

import com.trendradar.crossborder.dto.GlobalLocalResponse;
import com.trendradar.crossborder.dto.OpportunityResponse;
import com.trendradar.crossborder.dto.PropagationResponse;
import com.trendradar.keyword.domain.KeywordTrend;
import com.trendradar.keyword.domain.PeriodType;
import com.trendradar.keyword.repository.KeywordTrendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CrossBorderService {

    private static final int MAX_RESULTS = 20;
    private static final int GLOBAL_THRESHOLD = 3;

    private final KeywordTrendRepository keywordTrendRepository;

    /**
     * View 1: Keywords trending in other countries but NOT in mine.
     */
    public List<OpportunityResponse> findOpportunities(String countryCode) {
        OffsetDateTime since = OffsetDateTime.now().minusHours(24);
        List<KeywordTrend> allTrends = keywordTrendRepository
                .findByPeriodTypeAndPeriodStartAfter(PeriodType.HOUR, since);

        // Group by keyword -> list of trends
        Map<String, List<KeywordTrend>> byKeyword = allTrends.stream()
                .collect(Collectors.groupingBy(KeywordTrend::getKeyword));

        List<OpportunityResponse> opportunities = new ArrayList<>();

        for (Map.Entry<String, List<KeywordTrend>> entry : byKeyword.entrySet()) {
            String keyword = entry.getKey();
            List<KeywordTrend> trends = entry.getValue();

            Set<String> countries = trends.stream()
                    .map(KeywordTrend::getCountryCode)
                    .collect(Collectors.toSet());

            // Skip if my country already has this keyword
            if (countries.contains(countryCode)) {
                continue;
            }

            // At least 1 other country must have it
            if (countries.isEmpty()) {
                continue;
            }

            int totalVideoCount = trends.stream()
                    .mapToInt(KeywordTrend::getVideoCount)
                    .sum();
            long totalViews = trends.stream()
                    .mapToLong(KeywordTrend::getTotalViews)
                    .sum();

            opportunities.add(OpportunityResponse.builder()
                    .keyword(keyword)
                    .trendingCountries(new ArrayList<>(countries))
                    .targetCountry(countryCode)
                    .totalVideoCount(totalVideoCount)
                    .totalViews(totalViews)
                    .build());
        }

        // Sort by totalViews DESC, limit 20
        opportunities.sort(Comparator.comparingLong(OpportunityResponse::getTotalViews).reversed());

        return opportunities.stream()
                .limit(MAX_RESULTS)
                .toList();
    }

    /**
     * View 2: How a keyword spread from country to country.
     */
    public PropagationResponse findPropagation(String keyword) {
        List<KeywordTrend> trends = keywordTrendRepository
                .findByKeywordAndPeriodTypeOrderByPeriodStartDesc(keyword, PeriodType.HOUR);

        // For each country, find the EARLIEST appearance
        Map<String, OffsetDateTime> earliestByCountry = new LinkedHashMap<>();

        for (KeywordTrend trend : trends) {
            earliestByCountry.merge(
                    trend.getCountryCode(),
                    trend.getPeriodStart(),
                    (existing, candidate) -> candidate.isBefore(existing) ? candidate : existing
            );
        }

        // Sort countries by first appearance time
        List<PropagationResponse.PropagationStep> path = earliestByCountry.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(entry -> PropagationResponse.PropagationStep.builder()
                        .countryCode(entry.getKey())
                        .firstSeenAt(entry.getValue())
                        .build())
                .toList();

        return PropagationResponse.builder()
                .keyword(keyword)
                .propagationPath(path)
                .build();
    }

    /**
     * View 3: Keywords in 3+ countries (global) vs 1 country only (local).
     */
    public GlobalLocalResponse findGlobalVsLocal(String countryCode) {
        OffsetDateTime since = OffsetDateTime.now().minusHours(24);
        List<KeywordTrend> allTrends = keywordTrendRepository
                .findByPeriodTypeAndPeriodStartAfter(PeriodType.HOUR, since);

        // Group by keyword
        Map<String, List<KeywordTrend>> byKeyword = allTrends.stream()
                .collect(Collectors.groupingBy(KeywordTrend::getKeyword));

        List<GlobalLocalResponse.KeywordInfo> globalKeywords = new ArrayList<>();
        List<GlobalLocalResponse.KeywordInfo> localKeywords = new ArrayList<>();

        for (Map.Entry<String, List<KeywordTrend>> entry : byKeyword.entrySet()) {
            String keyword = entry.getKey();
            List<KeywordTrend> trends = entry.getValue();

            Set<String> countries = trends.stream()
                    .map(KeywordTrend::getCountryCode)
                    .collect(Collectors.toSet());

            int totalVideoCount = trends.stream()
                    .mapToInt(KeywordTrend::getVideoCount)
                    .sum();

            GlobalLocalResponse.KeywordInfo info = GlobalLocalResponse.KeywordInfo.builder()
                    .keyword(keyword)
                    .countries(new ArrayList<>(countries))
                    .videoCount(totalVideoCount)
                    .build();

            if (countries.size() >= GLOBAL_THRESHOLD) {
                globalKeywords.add(info);
            } else if (countries.size() == 1 && countries.contains(countryCode)) {
                localKeywords.add(info);
            }
        }

        // Sort by videoCount DESC, limit 20 each
        globalKeywords.sort(Comparator.comparingInt(GlobalLocalResponse.KeywordInfo::getVideoCount).reversed());
        localKeywords.sort(Comparator.comparingInt(GlobalLocalResponse.KeywordInfo::getVideoCount).reversed());

        return GlobalLocalResponse.builder()
                .globalKeywords(globalKeywords.stream().limit(MAX_RESULTS).toList())
                .localKeywords(localKeywords.stream().limit(MAX_RESULTS).toList())
                .build();
    }
}
