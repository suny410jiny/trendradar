package com.trendradar.trending.dto;

import com.trendradar.trending.domain.TrendingVideo;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class TrendingVideoResponse {

    private final String videoId;
    private final String title;
    private final String channelTitle;
    private final String countryCode;
    private final Integer categoryId;
    private final String categoryName;
    private final Integer rankPosition;
    private final Long viewCount;
    private final Long likeCount;
    private final Long commentCount;
    private final OffsetDateTime publishedAt;
    private final String thumbnailUrl;
    private final String duration;
    private final OffsetDateTime collectedAt;
    private final List<String> tags;

    public static TrendingVideoResponse from(TrendingVideo video) {
        return from(video, List.of());
    }

    public static TrendingVideoResponse from(TrendingVideo video, List<String> tags) {
        return TrendingVideoResponse.builder()
                .videoId(video.getVideoId())
                .title(video.getTitle())
                .channelTitle(video.getChannelTitle())
                .countryCode(video.getCountryCode())
                .categoryId(video.getCategoryId())
                .categoryName(com.trendradar.trending.domain.YouTubeCategory.nameOf(
                        video.getCategoryId() != null ? video.getCategoryId() : 0))
                .rankPosition(video.getRankPosition())
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .commentCount(video.getCommentCount())
                .publishedAt(video.getPublishedAt())
                .thumbnailUrl(video.getThumbnailUrl())
                .duration(video.getDuration())
                .collectedAt(video.getCollectedAt())
                .tags(tags)
                .build();
    }
}
