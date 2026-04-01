package com.trendradar.trending.service;

import com.trendradar.stats.service.DemographicInferenceService;
import com.trendradar.trending.domain.AlgorithmTag;
import com.trendradar.trending.domain.TrendingVideo;
import com.trendradar.trending.domain.ViewSnapshot;
import com.trendradar.trending.dto.CountryResponse;
import com.trendradar.trending.dto.TrendingVideoResponse;
import com.trendradar.trending.dto.ViewSnapshotResponse;
import com.trendradar.trending.repository.AlgorithmTagRepository;
import com.trendradar.trending.repository.CountryRepository;
import com.trendradar.trending.repository.TrendingVideoRepository;
import com.trendradar.trending.repository.ViewSnapshotRepository;
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
public class TrendingService {

    private final TrendingVideoRepository trendingVideoRepository;
    private final AlgorithmTagRepository algorithmTagRepository;
    private final ViewSnapshotRepository viewSnapshotRepository;
    private final CountryRepository countryRepository;
    private final DemographicInferenceService demographicService;

    public List<TrendingVideoResponse> getTrending(String countryCode, Integer categoryId, String tagType, int limit) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime from = now.minusHours(2);

        List<TrendingVideo> videos = trendingVideoRepository
                .findByCountryCodeAndCollectedAtBetweenOrderByRankPositionAsc(countryCode, from, now);

        // 카테고리 필터
        if (categoryId != null) {
            videos = videos.stream()
                    .filter(v -> categoryId.equals(v.getCategoryId()))
                    .toList();
        }

        // 태그 필터: 해당 태그를 가진 videoId 목록으로 필터링
        if (tagType != null && !tagType.isBlank()) {
            List<String> allVideoIds = videos.stream().map(TrendingVideo::getVideoId).toList();
            Set<String> taggedVideoIds = new HashSet<>(
                    algorithmTagRepository.findVideoIdsByTagType(allVideoIds, tagType));
            videos = videos.stream()
                    .filter(v -> taggedVideoIds.contains(v.getVideoId()))
                    .toList();
        }

        if (videos.size() > limit) {
            videos = videos.subList(0, limit);
        }

        if (videos.isEmpty()) {
            return Collections.emptyList();
        }

        // N+1 방지: Batch로 태그 조회 → Map 변환
        List<String> videoIds = videos.stream().map(TrendingVideo::getVideoId).toList();
        Map<String, List<String>> tagMap = algorithmTagRepository.findByVideoIdIn(videoIds)
                .stream()
                .collect(Collectors.groupingBy(
                        AlgorithmTag::getVideoId,
                        Collectors.mapping(AlgorithmTag::getTagType, Collectors.toList())
                ));

        // 순위 변동 계산: 이전 수집(2~4시간 전)과 비교
        Map<String, Integer> previousRankMap = getPreviousRankMap(countryCode, from);

        return videos.stream()
                .map(v -> {
                    // 태그 중복 제거
                    List<String> tags = tagMap.getOrDefault(v.getVideoId(), List.of())
                            .stream().distinct().toList();

                    // 순위 변동
                    Integer prevRank = previousRankMap.get(v.getVideoId());
                    Integer rankChange = prevRank != null ? prevRank - v.getRankPosition() : null;
                    String rankChangeType = prevRank == null ? "NEW" :
                            rankChange > 0 ? "UP" : rankChange < 0 ? "DOWN" : "SAME";

                    // 연령대 추론
                    String demographic = demographicService.inferPrimaryDemographic(v);

                    return TrendingVideoResponse.from(v, tags, prevRank, rankChange, rankChangeType, demographic);
                })
                .toList();
    }

    public List<ViewSnapshotResponse> getSnapshots(String videoId) {
        OffsetDateTime sevenDaysAgo = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);

        List<ViewSnapshot> snapshots = viewSnapshotRepository
                .findByVideoIdAndSnapshotAtAfterOrderBySnapshotAtAsc(videoId, sevenDaysAgo);

        if (snapshots.isEmpty()) {
            throw new NoSuchElementException("Video snapshots not found: " + videoId);
        }

        return snapshots.stream()
                .map(ViewSnapshotResponse::from)
                .toList();
    }

    public List<CountryResponse> getCountries() {
        return countryRepository.findAllByOrderByCodeAsc().stream()
                .map(CountryResponse::from)
                .toList();
    }

    /**
     * 이전 수집(2~4시간 전)의 videoId → 순위 매핑 조회
     */
    private Map<String, Integer> getPreviousRankMap(String countryCode, OffsetDateTime currentFrom) {
        OffsetDateTime prevFrom = currentFrom.minusHours(2);

        List<TrendingVideo> previous = trendingVideoRepository
                .findByCountryCodeAndCollectedAtBetweenOrderByRankPositionAsc(countryCode, prevFrom, currentFrom);

        return previous.stream()
                .collect(Collectors.toMap(
                        TrendingVideo::getVideoId,
                        TrendingVideo::getRankPosition,
                        (r1, r2) -> r1
                ));
    }
}
