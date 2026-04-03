package com.trendradar.trending.service;

import com.trendradar.trending.domain.TrendingVideo;
import com.trendradar.trending.repository.TrendingVideoRepository;
import com.trendradar.youtube.client.YouTubeApiClient;
import com.trendradar.youtube.dto.YouTubeResponse;
import com.trendradar.youtube.dto.YouTubeVideoItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.scheduling.annotation.Async;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendingCollectService {

    private final YouTubeApiClient youTubeApiClient;
    private final TrendingVideoRepository trendingVideoRepository;

    @Transactional
    public List<TrendingVideo> collect(String countryCode) {
        log.info("Collecting trending videos for country={}", countryCode);

        YouTubeResponse response = youTubeApiClient.fetchTrendingVideos(countryCode, 50);

        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            log.warn("No trending videos returned for country={}", countryCode);
            return Collections.emptyList();
        }

        OffsetDateTime collectedAt = OffsetDateTime.now(ZoneOffset.UTC);

        // 같은 시간대(1시간 이내)에 이미 수집된 데이터가 있으면 스킵
        OffsetDateTime oneHourAgo = collectedAt.minusHours(1);
        List<TrendingVideo> existing = trendingVideoRepository
                .findByCountryCodeAndCollectedAtBetweenOrderByRankPositionAsc(countryCode, oneHourAgo, collectedAt);
        if (!existing.isEmpty()) {
            log.info("Skipping collection for country={}, already collected {} videos within last hour", countryCode, existing.size());
            return existing;
        }

        List<TrendingVideo> videos = IntStream.range(0, response.getItems().size())
                .mapToObj(i -> toTrendingVideo(response.getItems().get(i), countryCode, i + 1, collectedAt))
                .toList();

        List<TrendingVideo> saved = trendingVideoRepository.saveAll(videos);
        log.info("Saved {} trending videos for country={}", saved.size(), countryCode);

        return saved;
    }

    @Async("trendingCollectExecutor")
    public CompletableFuture<List<TrendingVideo>> collectAsync(String countryCode) {
        List<TrendingVideo> result = collect(countryCode);
        return CompletableFuture.completedFuture(result);
    }

    private TrendingVideo toTrendingVideo(YouTubeVideoItem item, String countryCode,
                                           int rank, OffsetDateTime collectedAt) {
        YouTubeVideoItem.Snippet snippet = item.getSnippet();
        YouTubeVideoItem.Statistics stats = item.getStatistics();
        YouTubeVideoItem.ContentDetails content = item.getContentDetails();

        return TrendingVideo.builder()
                .videoId(item.getId())
                .title(snippet != null ? snippet.getTitle() : "")
                .channelTitle(snippet != null ? snippet.getChannelTitle() : "")
                .countryCode(countryCode)
                .categoryId(snippet != null && snippet.getCategoryId() != null
                        ? Integer.parseInt(snippet.getCategoryId()) : null)
                .rankPosition(rank)
                .viewCount(parseLong(stats != null ? stats.getViewCount() : null))
                .likeCount(parseLong(stats != null ? stats.getLikeCount() : null))
                .commentCount(parseLong(stats != null ? stats.getCommentCount() : null))
                .publishedAt(parseDateTime(snippet != null ? snippet.getPublishedAt() : null))
                .thumbnailUrl(extractThumbnailUrl(snippet))
                .duration(content != null ? content.getDuration() : null)
                .channelId(snippet != null ? snippet.getChannelId() : null)
                .collectedAt(collectedAt)
                .youtubeTags(extractYoutubeTags(snippet))
                .isShort(isShortVideo(content))
                .build();
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private OffsetDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractYoutubeTags(YouTubeVideoItem.Snippet snippet) {
        if (snippet == null || snippet.getTags() == null || snippet.getTags().isEmpty()) {
            return null;
        }
        return String.join(",", snippet.getTags());
    }

    private boolean isShortVideo(YouTubeVideoItem.ContentDetails content) {
        if (content == null || content.getDuration() == null) return false;
        try {
            Duration duration = Duration.parse(content.getDuration());
            return duration.getSeconds() <= 60;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractThumbnailUrl(YouTubeVideoItem.Snippet snippet) {
        if (snippet == null || snippet.getThumbnails() == null
                || snippet.getThumbnails().getHigh() == null) {
            return null;
        }
        return snippet.getThumbnails().getHigh().getUrl();
    }
}
