package com.trendradar.tag.service;

import com.trendradar.tag.domain.TagType;
import com.trendradar.trending.domain.AlgorithmTag;
import com.trendradar.trending.domain.TrendingVideo;
import com.trendradar.trending.repository.AlgorithmTagRepository;
import com.trendradar.trending.repository.TrendingVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlgorithmTagService {

    private static final long SURGE_THRESHOLD = 500_000L;
    private static final long NEW_ENTRY_HOURS = 48L;
    private static final int LONG_RUN_DAYS = 7;
    private static final int GLOBAL_COUNTRIES = 3;
    private static final int COMEBACK_DAYS = 30;
    private static final double TOP_PERCENT = 0.10;

    private final TrendingVideoRepository trendingVideoRepository;
    private final AlgorithmTagRepository algorithmTagRepository;

    @Transactional
    public List<AlgorithmTag> calculateTags(List<TrendingVideo> videos, OffsetDateTime collectedAt) {
        if (videos == null || videos.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> videoIds = videos.stream()
                .map(TrendingVideo::getVideoId).toList();

        // Batch 쿼리: 24시간 전 데이터 (SURGE용)
        Map<String, Long> previousViewCounts = getPreviousViewCounts(videoIds, collectedAt);

        // Batch 쿼리: LONG_RUN videoIds
        Set<String> longRunIds = new HashSet<>(
                trendingVideoRepository.findLongRunVideoIds(videoIds, collectedAt.minusDays(LONG_RUN_DAYS)));

        // Batch 쿼리: GLOBAL videoIds
        Set<String> globalIds = new HashSet<>(
                trendingVideoRepository.findGlobalVideoIds(videoIds,
                        collectedAt.minusHours(2), collectedAt.plusHours(2)));

        // 인메모리: HOT_COMMENT 상위 10% 커트라인
        double hotCommentCutoff = calculateRatioCutoff(videos, this::commentRatio);

        // 인메모리: HIGH_ENGAGE 상위 10% 커트라인
        double highEngageCutoff = calculateRatioCutoff(videos, this::likeRatio);

        // 기존 태그 삭제 후 재계산
        algorithmTagRepository.deleteByVideoIdIn(videoIds);

        // 각 영상에 대해 태그 계산
        List<AlgorithmTag> allTags = new ArrayList<>();

        for (TrendingVideo video : videos) {
            List<AlgorithmTag> videoTags = new ArrayList<>();

            // SURGE
            Long prevView = previousViewCounts.get(video.getVideoId());
            if (prevView != null && video.getViewCount() - prevView >= SURGE_THRESHOLD) {
                videoTags.add(createTag(video.getVideoId(), TagType.SURGE, collectedAt));
            }

            // NEW_ENTRY
            if (video.getPublishedAt() != null) {
                long hoursSincePublish = Duration.between(video.getPublishedAt(), collectedAt).toHours();
                if (hoursSincePublish >= 0 && hoursSincePublish <= NEW_ENTRY_HOURS) {
                    videoTags.add(createTag(video.getVideoId(), TagType.NEW_ENTRY, collectedAt));
                }
            }

            // HOT_COMMENT
            if (commentRatio(video) >= hotCommentCutoff && hotCommentCutoff > 0) {
                videoTags.add(createTag(video.getVideoId(), TagType.HOT_COMMENT, collectedAt));
            }

            // HIGH_ENGAGE
            if (likeRatio(video) >= highEngageCutoff && highEngageCutoff > 0) {
                videoTags.add(createTag(video.getVideoId(), TagType.HIGH_ENGAGE, collectedAt));
            }

            // LONG_RUN
            if (longRunIds.contains(video.getVideoId())) {
                videoTags.add(createTag(video.getVideoId(), TagType.LONG_RUN, collectedAt));
            }

            // GLOBAL
            if (globalIds.contains(video.getVideoId())) {
                videoTags.add(createTag(video.getVideoId(), TagType.GLOBAL, collectedAt));
            }

            // COMEBACK
            if (video.getPublishedAt() != null) {
                long daysSincePublish = Duration.between(video.getPublishedAt(), collectedAt).toDays();
                if (daysSincePublish > COMEBACK_DAYS) {
                    videoTags.add(createTag(video.getVideoId(), TagType.COMEBACK, collectedAt));
                }
            }

            allTags.addAll(videoTags);
        }

        List<AlgorithmTag> saved = algorithmTagRepository.saveAll(allTags);
        log.info("Calculated {} tags for {} videos", saved.size(), videos.size());
        return saved;
    }

    private Map<String, Long> getPreviousViewCounts(List<String> videoIds, OffsetDateTime collectedAt) {
        OffsetDateTime from = collectedAt.minusHours(25);
        OffsetDateTime to = collectedAt.minusHours(1);

        List<TrendingVideo> previous = trendingVideoRepository
                .findByVideoIdInAndCollectedAtBetween(videoIds, from, to);

        // 가장 최근 데이터만 사용 (같은 videoId가 여러 개일 수 있음)
        return previous.stream()
                .collect(Collectors.toMap(
                        TrendingVideo::getVideoId,
                        TrendingVideo::getViewCount,
                        (v1, v2) -> v2  // 나중 것 사용
                ));
    }

    @FunctionalInterface
    private interface RatioFunction {
        double apply(TrendingVideo video);
    }

    private double calculateRatioCutoff(List<TrendingVideo> videos, RatioFunction ratioFn) {
        if (videos.size() < 2) return Double.MAX_VALUE;

        List<Double> ratios = videos.stream()
                .map(ratioFn::apply)
                .filter(r -> r > 0)
                .sorted(Comparator.reverseOrder())
                .toList();

        if (ratios.isEmpty()) return Double.MAX_VALUE;

        int top10Index = Math.max(0, (int) Math.ceil(ratios.size() * TOP_PERCENT) - 1);
        return ratios.get(top10Index);
    }

    private double commentRatio(TrendingVideo video) {
        if (video.getViewCount() == null || video.getViewCount() == 0) return 0.0;
        if (video.getCommentCount() == null) return 0.0;
        return (double) video.getCommentCount() / video.getViewCount();
    }

    private double likeRatio(TrendingVideo video) {
        if (video.getViewCount() == null || video.getViewCount() == 0) return 0.0;
        if (video.getLikeCount() == null) return 0.0;
        return (double) video.getLikeCount() / video.getViewCount();
    }

    private AlgorithmTag createTag(String videoId, TagType tagType, OffsetDateTime calculatedAt) {
        return AlgorithmTag.builder()
                .videoId(videoId)
                .tagType(tagType.name())
                .calculatedAt(calculatedAt)
                .build();
    }
}
