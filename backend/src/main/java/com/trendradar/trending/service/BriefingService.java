package com.trendradar.trending.service;

import com.trendradar.trending.domain.Country;
import com.trendradar.trending.dto.BriefingResponse;
import com.trendradar.trending.dto.TrendingVideoResponse;
import com.trendradar.trending.repository.CountryRepository;
import com.trendradar.youtube.client.ClaudeApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BriefingService {

    private final TrendingService trendingService;
    private final CountryRepository countryRepository;
    private final ClaudeApiClient claudeApiClient;

    public BriefingResponse generateBriefing(String countryCode) {
        Country country = countryRepository.findById(countryCode)
                .orElseThrow(() -> new NoSuchElementException("Country not found: " + countryCode));

        List<TrendingVideoResponse> topVideos = trendingService.getTrending(countryCode, null, null, 10);

        String prompt = buildPrompt(country.getNameKo(), topVideos);
        String summary = claudeApiClient.generateBriefing(prompt);

        return BriefingResponse.builder()
                .country(countryCode)
                .countryName(country.getNameKo())
                .summary(summary)
                .topVideos(topVideos)
                .generatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }

    private String buildPrompt(String countryName, List<TrendingVideoResponse> videos) {
        String videoList = videos.stream()
                .map(v -> String.format("%d위: %s (%s) - 조회수 %,d",
                        v.getRankPosition(), v.getTitle(), v.getChannelTitle(),
                        v.getViewCount() != null ? v.getViewCount() : 0))
                .collect(Collectors.joining("\n"));

        return String.format(
                "다음은 %s YouTube 트렌딩 TOP %d입니다.\n\n%s\n\n3줄로 핵심 트렌드를 한국어로 요약해주세요.",
                countryName, videos.size(), videoList);
    }
}
