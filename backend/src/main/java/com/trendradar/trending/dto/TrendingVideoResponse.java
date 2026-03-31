package com.trendradar.trending.dto;

import com.trendradar.trending.domain.TrendingVideo;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class TrendingVideoResponse {

    private final String videoId;
    private final String title;
    private final String channelTitle;
    private final String countryCode;
    private final Integer categoryId;
    private final Integer rankPosition;
    private final Long viewCount;
    private final Long likeCount;
    private final Long commentCount;
    private final OffsetDateTime publishedAt;
    private final String thumbnailUrl;
    private final String duration;
    private final OffsetDateTime collectedAt;

    public static TrendingVideoResponse from(TrendingVideo video) {
        return TrendingVideoResponse.builder()
                .videoId(video.getVideoId())
                .title(video.getTitle())
                .channelTitle(video.getChannelTitle())
                .countryCode(video.getCountryCode())
                .categoryId(video.getCategoryId())
                .rankPosition(video.getRankPosition())
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .commentCount(video.getCommentCount())
                .publishedAt(video.getPublishedAt())
                .thumbnailUrl(video.getThumbnailUrl())
                .duration(video.getDuration())
                .collectedAt(video.getCollectedAt())
                .build();
    }
}
