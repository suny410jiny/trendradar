package com.trendradar.channel.service;

import com.trendradar.channel.domain.Channel;
import com.trendradar.channel.dto.ChannelRankingResponse;
import com.trendradar.channel.repository.ChannelRepository;
import com.trendradar.trending.domain.TrendingVideo;
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
@Slf4j
@Transactional(readOnly = true)
public class ChannelRankingService {

    private static final List<String> ALL_COUNTRIES = List.of("KR", "US", "JP", "GB", "DE");
    private static final double DARKHORSE_THRESHOLD = 10.0;

    private final TrendingVideoRepository trendingVideoRepository;
    private final ChannelRepository channelRepository;

    /**
     * 채널 서지 스코어 기반 랭킹 조회
     *
     * @param countryCode 국가 코드 (ALL이면 5개국 통합)
     * @param limit       상위 N개 반환
     * @return surgeScore 내림차순 정렬된 채널 랭킹
     */
    public List<ChannelRankingResponse> getRanking(String countryCode, int limit) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime from24h = now.minusHours(24);

        // 1. 채널 ID 목록 수집
        List<String> channelIds = collectChannelIds(countryCode, from24h, now);
        if (channelIds.isEmpty()) {
            return List.of();
        }

        // 2. 해당 채널들의 트렌딩 영상 Batch 조회
        List<TrendingVideo> trendingVideos = trendingVideoRepository
                .findByChannelIdInAndCollectedAtBetween(channelIds, from24h, now);
        if (trendingVideos.isEmpty()) {
            return List.of();
        }

        // 3. 채널별 그룹핑
        Map<String, List<TrendingVideo>> videosByChannel = trendingVideos.stream()
                .collect(Collectors.groupingBy(TrendingVideo::getChannelId));

        // 4. 24시간 전 데이터 Batch 조회 (growth rate 계산용)
        List<String> videoIds = trendingVideos.stream()
                .map(TrendingVideo::getVideoId)
                .distinct()
                .toList();
        OffsetDateTime prev48h = from24h.minusHours(24);
        List<TrendingVideo> previousVideos = trendingVideoRepository
                .findByVideoIdInAndCollectedAtBetween(videoIds, prev48h, from24h);
        Map<String, TrendingVideo> previousVideoMap = buildPreviousVideoMap(previousVideos);

        // 5. 채널 정보 Batch 조회
        List<Channel> channels = channelRepository.findByChannelIdIn(channelIds);
        Map<String, Channel> channelMap = channels.stream()
                .collect(Collectors.toMap(Channel::getChannelId, c -> c));

        // 6. 채널별 5가지 원시 지표 계산
        Map<String, double[]> rawMetrics = new LinkedHashMap<>();
        for (String chId : videosByChannel.keySet()) {
            List<TrendingVideo> chVideos = videosByChannel.get(chId);
            Channel channel = channelMap.get(chId);
            double[] metrics = calculateRawMetrics(chVideos, channel, previousVideoMap);
            rawMetrics.put(chId, metrics);
        }

        // 7. Min-Max 정규화
        Map<String, double[]> normalizedMetrics = normalizeMetrics(rawMetrics);

        // 8. surgeScore 계산 + DTO 변환 + 정렬
        List<ChannelRankingResponse> result = new ArrayList<>();
        for (Map.Entry<String, double[]> entry : normalizedMetrics.entrySet()) {
            String chId = entry.getKey();
            double[] norm = entry.getValue();
            double[] raw = rawMetrics.get(chId);

            double surgeScore = 0.25 * norm[0]   // trending frequency
                    + 0.20 * norm[1]              // rank score
                    + 0.20 * norm[2]              // view growth rate
                    + 0.15 * norm[3]              // engagement rate
                    + 0.20 * norm[4];             // burst ratio

            Channel channel = channelMap.get(chId);
            List<TrendingVideo> chVideos = videosByChannel.get(chId);
            int trendingVideoCount = (int) chVideos.stream()
                    .map(TrendingVideo::getVideoId)
                    .distinct()
                    .count();
            double burstRatio = raw[4];
            boolean darkhorse = burstRatio >= DARKHORSE_THRESHOLD;

            String grade = assignGrade(surgeScore);
            String gradeLabel = assignGradeLabel(grade);

            ChannelRankingResponse response = ChannelRankingResponse.builder()
                    .channelId(chId)
                    .title(channel != null ? channel.getTitle() : getChannelTitleFromVideos(chVideos))
                    .thumbnailUrl(channel != null ? channel.getThumbnailUrl() : null)
                    .subscriberCount(channel != null ? channel.getSubscriberCount() : null)
                    .totalViewCount(channel != null ? channel.getTotalViewCount() : null)
                    .surgeScore(Math.round(surgeScore * 10000.0) / 10000.0)
                    .grade(grade)
                    .gradeLabel(gradeLabel)
                    .darkhorse(darkhorse)
                    .trendingVideoCount(trendingVideoCount)
                    .burstRatio(Math.round(burstRatio * 100.0) / 100.0)
                    .build();

            result.add(response);
        }

        result.sort(Comparator.comparingDouble(ChannelRankingResponse::getSurgeScore).reversed());

        return result.stream()
                .limit(limit)
                .toList();
    }

    /**
     * 국가 코드에 따른 채널 ID 목록 수집
     */
    private List<String> collectChannelIds(String countryCode, OffsetDateTime from, OffsetDateTime to) {
        if ("ALL".equalsIgnoreCase(countryCode)) {
            Set<String> allChannelIds = new LinkedHashSet<>();
            for (String cc : ALL_COUNTRIES) {
                List<String> ids = trendingVideoRepository
                        .findDistinctChannelIdsByCountryCode(cc, from, to);
                allChannelIds.addAll(ids);
            }
            return new ArrayList<>(allChannelIds);
        }
        return trendingVideoRepository.findDistinctChannelIdsByCountryCode(countryCode, from, to);
    }

    /**
     * 채널별 5가지 원시 지표 계산
     *
     * @return double[5] = { trendingFreq, rankScore, growthRate, engagementRate, burstRatio }
     */
    private double[] calculateRawMetrics(List<TrendingVideo> chVideos, Channel channel,
                                          Map<String, TrendingVideo> previousVideoMap) {
        // 1. Trending Frequency: 고유 video_id 수
        long trendingFreq = chVideos.stream()
                .map(TrendingVideo::getVideoId)
                .distinct()
                .count();

        // 2. Rank Score: AVG((51 - rank) / 50)
        double rankScore = chVideos.stream()
                .mapToDouble(v -> (51.0 - safeInt(v.getRankPosition(), 50)) / 50.0)
                .average()
                .orElse(0.0);

        // 3. View Growth Rate: AVG((current - prev) / prev)
        double growthRate = calculateGrowthRate(chVideos, previousVideoMap);

        // 4. Engagement Rate: AVG((likes + comments) / views)
        double engagementRate = chVideos.stream()
                .mapToDouble(v -> {
                    long views = safeLong(v.getViewCount(), 0L);
                    if (views == 0) return 0.0;
                    long likes = safeLong(v.getLikeCount(), 0L);
                    long comments = safeLong(v.getCommentCount(), 0L);
                    return (double) (likes + comments) / views;
                })
                .average()
                .orElse(0.0);

        // 5. Burst Ratio: AVG(video_views) / subscriber_count
        double avgViews = chVideos.stream()
                .mapToLong(v -> safeLong(v.getViewCount(), 0L))
                .average()
                .orElse(0.0);
        long subscriberCount = (channel != null && channel.getSubscriberCount() != null
                && channel.getSubscriberCount() > 0)
                ? channel.getSubscriberCount() : 1L;
        double burstRatio = avgViews / subscriberCount;

        return new double[]{trendingFreq, rankScore, growthRate, engagementRate, burstRatio};
    }

    /**
     * View Growth Rate 계산
     */
    private double calculateGrowthRate(List<TrendingVideo> chVideos,
                                        Map<String, TrendingVideo> previousVideoMap) {
        List<Double> growthRates = new ArrayList<>();

        // 채널 영상들의 고유 videoId별로 growth rate 계산
        Map<String, List<TrendingVideo>> byVideoId = chVideos.stream()
                .collect(Collectors.groupingBy(TrendingVideo::getVideoId));

        for (Map.Entry<String, List<TrendingVideo>> entry : byVideoId.entrySet()) {
            String videoId = entry.getKey();
            TrendingVideo prevVideo = previousVideoMap.get(videoId);
            if (prevVideo == null) {
                growthRates.add(0.0);
                continue;
            }
            long prevViews = safeLong(prevVideo.getViewCount(), 0L);
            if (prevViews == 0) {
                growthRates.add(0.0);
                continue;
            }
            // 현재 조회수: 해당 비디오의 최신 데이터 사용
            long currentViews = entry.getValue().stream()
                    .mapToLong(v -> safeLong(v.getViewCount(), 0L))
                    .max()
                    .orElse(0L);
            double rate = (double) (currentViews - prevViews) / prevViews;
            growthRates.add(rate);
        }

        if (growthRates.isEmpty()) return 0.0;
        return growthRates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    /**
     * 24시간 전 영상 데이터를 videoId 기준으로 Map 변환 (가장 최근 것 사용)
     */
    private Map<String, TrendingVideo> buildPreviousVideoMap(List<TrendingVideo> previousVideos) {
        Map<String, TrendingVideo> map = new HashMap<>();
        for (TrendingVideo v : previousVideos) {
            String videoId = v.getVideoId();
            TrendingVideo existing = map.get(videoId);
            if (existing == null || v.getCollectedAt().isAfter(existing.getCollectedAt())) {
                map.put(videoId, v);
            }
        }
        return map;
    }

    /**
     * Min-Max 정규화 (모든 채널에 대해)
     */
    private Map<String, double[]> normalizeMetrics(Map<String, double[]> rawMetrics) {
        int numIndicators = 5;
        double[] mins = new double[numIndicators];
        double[] maxs = new double[numIndicators];
        Arrays.fill(mins, Double.MAX_VALUE);
        Arrays.fill(maxs, Double.MIN_VALUE);

        for (double[] metrics : rawMetrics.values()) {
            for (int i = 0; i < numIndicators; i++) {
                mins[i] = Math.min(mins[i], metrics[i]);
                maxs[i] = Math.max(maxs[i], metrics[i]);
            }
        }

        Map<String, double[]> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, double[]> entry : rawMetrics.entrySet()) {
            double[] raw = entry.getValue();
            double[] norm = new double[numIndicators];
            for (int i = 0; i < numIndicators; i++) {
                double range = maxs[i] - mins[i];
                norm[i] = (range == 0) ? 0.0 : (raw[i] - mins[i]) / range;
            }
            normalized.put(entry.getKey(), norm);
        }

        return normalized;
    }

    private String assignGrade(double surgeScore) {
        if (surgeScore >= 0.9) return "S";
        if (surgeScore >= 0.75) return "A";
        if (surgeScore >= 0.55) return "B";
        if (surgeScore >= 0.35) return "C";
        return "D";
    }

    private String assignGradeLabel(String grade) {
        return switch (grade) {
            case "S" -> "지금 가장 핫한 채널!";
            case "A" -> "주목할 만한 성장세!";
            case "B" -> "꾸준히 성장하는 채널";
            case "C" -> "잠재력 있는 채널";
            default -> "트렌딩에 등장한 채널";
        };
    }

    private String getChannelTitleFromVideos(List<TrendingVideo> videos) {
        return videos.stream()
                .map(TrendingVideo::getChannelTitle)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Unknown");
    }

    private long safeLong(Long value, long defaultValue) {
        return value != null ? value : defaultValue;
    }

    private int safeInt(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }
}
