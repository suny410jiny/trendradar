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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
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

        List<TrendingVideo> videos = IntStream.range(0, response.getItems().size())
                .mapToObj(i -> toTrendingVideo(response.getItems().get(i), countryCode, i + 1, collectedAt))
                .toList();

        List<TrendingVideo> saved = trendingVideoRepository.saveAll(videos);
        log.info("Saved {} trending videos for country={}", saved.size(), countryCode);

        return saved;
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
                .collectedAt(collectedAt)
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

    private String extractThumbnailUrl(YouTubeVideoItem.Snippet snippet) {
        if (snippet == null || snippet.getThumbnails() == null
                || snippet.getThumbnails().getHigh() == null) {
            return null;
        }
        return snippet.getThumbnails().getHigh().getUrl();
    }
}
