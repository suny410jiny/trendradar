package com.trendradar.stats.service;

import com.trendradar.stats.dto.*;
import com.trendradar.tag.domain.TagType;
import com.trendradar.trending.domain.AlgorithmTag;
import com.trendradar.trending.domain.Country;
import com.trendradar.trending.domain.TrendingVideo;
import com.trendradar.trending.domain.YouTubeCategory;
import com.trendradar.trending.repository.AlgorithmTagRepository;
import com.trendradar.trending.repository.CountryRepository;
import com.trendradar.trending.repository.TrendingVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class StatsService {

    private final TrendingVideoRepository trendingVideoRepository;
    private final AlgorithmTagRepository algorithmTagRepository;
    private final CountryRepository countryRepository;
    private final DemographicInferenceService demographicService;

    /**
     * 대시보드용 통합 통계 조회
     */
    public StatsOverviewResponse getOverview(String countryCode) {
        Country country = countryRepository.findById(countryCode)
                .orElseThrow(() -> new NoSuchElementException("Country not found: " + countryCode));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime twoHoursAgo = now.minusHours(2);
        OffsetDateTime oneDayAgo = now.minusHours(24);

        // 현재 트렌딩 영상 (최근 2시간)
        List<TrendingVideo> currentVideos = trendingVideoRepository
                .findByCountryCodeAndCollectedAtBetweenOrderByRankPositionAsc(countryCode, twoHoursAgo, now);

        // 24시간 내 고유 영상 수 (TOP 50이 순환되므로 실제 50개 이상)
        List<TrendingVideo> videos24h = trendingVideoRepository
                .findByCountryCodeAndCollectedAtBetweenOrderByRankPositionAsc(countryCode, oneDayAgo, now);
        long uniqueVideos24h = videos24h.stream()
                .map(TrendingVideo::getVideoId)
                .distinct()
                .count();

        // 태그 정보 Batch 조회
        List<String> videoIds = currentVideos.stream().map(TrendingVideo::getVideoId).toList();
        Map<String, List<String>> tagMap = algorithmTagRepository.findByVideoIdIn(videoIds)
                .stream()
                .collect(Collectors.groupingBy(
                        AlgorithmTag::getVideoId,
                        Collectors.mapping(AlgorithmTag::getTagType, Collectors.toList())
                ));

        // KPI 계산
        int totalVideos = currentVideos.size();
        long newEntryCount = tagMap.values().stream()
                .flatMap(Collection::stream)
                .filter("NEW_ENTRY"::equals)
                .count();
        long surgeCount = tagMap.values().stream()
                .flatMap(Collection::stream)
                .filter("SURGE"::equals)
                .count();

        long totalViews = currentVideos.stream()
                .mapToLong(v -> v.getViewCount() != null ? v.getViewCount() : 0)
                .sum();

        double avgEngagement = currentVideos.stream()
                .filter(v -> v.getViewCount() != null && v.getViewCount() > 0 && v.getLikeCount() != null)
                .mapToDouble(v -> (double) v.getLikeCount() / v.getViewCount() * 100)
                .average()
                .orElse(0.0);

        // 카테고리 분포
        List<CategoryStatResponse> categoryStats = calculateCategoryDistribution(currentVideos);

        // 태그 분포
        List<TagStatResponse> tagStats = calculateTagDistribution(tagMap, totalVideos);

        // 연령대 분포
        List<DemographicStatResponse> demographicStats = calculateDemographicDistribution(currentVideos);

        return StatsOverviewResponse.builder()
                .countryCode(countryCode)
                .countryName(country.getNameKo())
                .totalVideos(totalVideos)
                .newEntryCount((int) newEntryCount)
                .surgeCount((int) surgeCount)
                .avgEngagementRate(Math.round(avgEngagement * 100.0) / 100.0)
                .totalViews(totalViews)
                .uniqueVideos24h((int) uniqueVideos24h)
                .categoryDistribution(categoryStats)
                .tagDistribution(tagStats)
                .demographicDistribution(demographicStats)
                .generatedAt(now)
                .build();
    }

    /**
     * 순위 변동 정보 계산
     */
    public Map<String, RankChangeInfo> getRankChanges(String countryCode) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime twoHoursAgo = now.minusHours(2);
        OffsetDateTime fourHoursAgo = now.minusHours(4);

        // 현재 수집
        List<TrendingVideo> current = trendingVideoRepository
                .findByCountryCodeAndCollectedAtBetweenOrderByRankPositionAsc(countryCode, twoHoursAgo, now);

        // 이전 수집 (2~4시간 전)
        List<TrendingVideo> previous = trendingVideoRepository
                .findByCountryCodeAndCollectedAtBetweenOrderByRankPositionAsc(countryCode, fourHoursAgo, twoHoursAgo);

        // 이전 수집의 videoId → rank 매핑
        Map<String, Integer> previousRankMap = previous.stream()
                .collect(Collectors.toMap(
                        TrendingVideo::getVideoId,
                        TrendingVideo::getRankPosition,
                        (r1, r2) -> r1 // 중복 시 첫 번째 사용
                ));

        Map<String, RankChangeInfo> rankChanges = new LinkedHashMap<>();
        for (TrendingVideo video : current) {
            String videoId = video.getVideoId();
            Integer prevRank = previousRankMap.get(videoId);

            RankChangeInfo info;
            if (prevRank == null) {
                info = RankChangeInfo.newEntry(videoId, video.getRankPosition());
            } else {
                info = RankChangeInfo.calculate(videoId, video.getRankPosition(), prevRank);
            }
            rankChanges.put(videoId, info);
        }

        return rankChanges;
    }

    private List<CategoryStatResponse> calculateCategoryDistribution(List<TrendingVideo> videos) {
        Map<Integer, List<TrendingVideo>> grouped = videos.stream()
                .filter(v -> v.getCategoryId() != null)
                .collect(Collectors.groupingBy(TrendingVideo::getCategoryId));

        int total = videos.size();

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<TrendingVideo> categoryVideos = entry.getValue();
                    long totalCategoryViews = categoryVideos.stream()
                            .mapToLong(v -> v.getViewCount() != null ? v.getViewCount() : 0)
                            .sum();
                    double avgViews = categoryVideos.stream()
                            .mapToLong(v -> v.getViewCount() != null ? v.getViewCount() : 0)
                            .average()
                            .orElse(0.0);

                    return CategoryStatResponse.builder()
                            .categoryId(entry.getKey())
                            .categoryName(YouTubeCategory.nameOf(entry.getKey()))
                            .videoCount(categoryVideos.size())
                            .totalViews(totalCategoryViews)
                            .avgViews(Math.round(avgViews))
                            .percentage(total > 0 ? Math.round((double) categoryVideos.size() / total * 1000.0) / 10.0 : 0)
                            .build();
                })
                .sorted(Comparator.comparingInt(CategoryStatResponse::getVideoCount).reversed())
                .toList();
    }

    private List<TagStatResponse> calculateTagDistribution(Map<String, List<String>> tagMap, int totalVideos) {
        // 모든 태그를 평탄화하여 개수 세기
        Map<String, Long> tagCounts = tagMap.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        return Arrays.stream(TagType.values())
                .map(tagType -> {
                    long count = tagCounts.getOrDefault(tagType.name(), 0L);
                    return TagStatResponse.builder()
                            .tagType(tagType.name())
                            .tagLabel(tagType.getLabel())
                            .videoCount((int) count)
                            .percentage(totalVideos > 0 ? Math.round((double) count / totalVideos * 1000.0) / 10.0 : 0)
                            .build();
                })
                .sorted(Comparator.comparingInt(TagStatResponse::getVideoCount).reversed())
                .toList();
    }

    private List<DemographicStatResponse> calculateDemographicDistribution(List<TrendingVideo> videos) {
        Map<String, List<TrendingVideo>> grouped = demographicService.groupByDemographic(videos);
        int total = videos.size();

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<TrendingVideo> ageVideos = entry.getValue();

                    // 해당 연령대의 주요 카테고리 Top 3
                    List<String> topCategories = ageVideos.stream()
                            .filter(v -> v.getCategoryId() != null)
                            .collect(Collectors.groupingBy(TrendingVideo::getCategoryId, Collectors.counting()))
                            .entrySet().stream()
                            .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                            .limit(3)
                            .map(e -> YouTubeCategory.nameOf(e.getKey()))
                            .toList();

                    return DemographicStatResponse.builder()
                            .ageGroup(entry.getKey())
                            .videoCount(ageVideos.size())
                            .percentage(total > 0 ? Math.round((double) ageVideos.size() / total * 1000.0) / 10.0 : 0)
                            .topCategories(topCategories)
                            .build();
                })
                .toList();
    }

    /**
     * 트렌딩 키워드 분석 (YouTube 원본 태그 기반)
     */
    public List<TrendingKeywordResponse> getTrendingKeywords(String countryCode, int limit) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime twoHoursAgo = now.minusHours(2);

        List<TrendingVideo> videos = trendingVideoRepository
                .findByCountryCodeAndCollectedAtBetweenOrderByRankPositionAsc(countryCode, twoHoursAgo, now);

        // 모든 YouTube 태그 수집 및 빈도 계산
        Map<String, Long> keywordCounts = videos.stream()
                .flatMap(v -> v.getYoutubeTagList().stream())
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        int totalVideos = videos.size();

        return keywordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> TrendingKeywordResponse.builder()
                        .keyword(entry.getKey())
                        .count(entry.getValue().intValue())
                        .percentage(totalVideos > 0 ? Math.round((double) entry.getValue() / totalVideos * 1000.0) / 10.0 : 0)
                        .build())
                .toList();
    }

    /**
     * Shorts vs 일반 영상 비율
     */
    public Map<String, Object> getShortsStats(String countryCode) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime twoHoursAgo = now.minusHours(2);

        List<TrendingVideo> videos = trendingVideoRepository
                .findByCountryCodeAndCollectedAtBetweenOrderByRankPositionAsc(countryCode, twoHoursAgo, now);

        long shortsCount = videos.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsShort()))
                .count();
        long regularCount = videos.size() - shortsCount;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalVideos", videos.size());
        result.put("shortsCount", shortsCount);
        result.put("regularCount", regularCount);
        result.put("shortsPercentage", videos.isEmpty() ? 0 : Math.round((double) shortsCount / videos.size() * 1000.0) / 10.0);
        return result;
    }
}
