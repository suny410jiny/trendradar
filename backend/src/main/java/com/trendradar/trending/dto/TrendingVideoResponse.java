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

    // 순위 변동 정보
    private final Integer previousRank;
    private final Integer rankChange;
    private final String rankChangeType;

    // 타겟 연령대 추론
    private final String targetDemographic;

    // YouTube 원본 태그/키워드
    private final List<String> youtubeTags;

    // Shorts 여부
    private final Boolean isShort;

    public static TrendingVideoResponse from(TrendingVideo video) {
        return from(video, List.of());
    }

    public static TrendingVideoResponse from(TrendingVideo video, List<String> tags) {
        return from(video, tags, null, null, null, null);
    }

    public static TrendingVideoResponse from(TrendingVideo video, List<String> tags,
                                              Integer previousRank, Integer rankChange,
                                              String rankChangeType, String targetDemographic) {
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
                .previousRank(previousRank)
                .rankChange(rankChange)
                .rankChangeType(rankChangeType)
                .targetDemographic(targetDemographic)
                .youtubeTags(video.getYoutubeTagList())
                .isShort(video.getIsShort())
                .build();
    }
}
