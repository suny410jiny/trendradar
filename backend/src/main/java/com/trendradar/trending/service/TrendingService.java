package com.trendradar.trending.service;

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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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

    public List<TrendingVideoResponse> getTrending(String countryCode, Integer categoryId, int limit) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime from = now.minusHours(2);

        List<TrendingVideo> videos = trendingVideoRepository
                .findByCountryCodeAndCollectedAtBetweenOrderByRankPositionAsc(countryCode, from, now);

        if (categoryId != null) {
            videos = videos.stream()
                    .filter(v -> categoryId.equals(v.getCategoryId()))
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

        return videos.stream()
                .map(v -> TrendingVideoResponse.from(v, tagMap.getOrDefault(v.getVideoId(), List.of())))
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
}
