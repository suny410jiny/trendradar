package com.trendradar.trending.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "trending_videos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrendingVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false, length = 20)
    private String videoId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "channel_title", length = 200)
    private String channelTitle;

    @Column(name = "country_code", nullable = false, length = 5)
    private String countryCode;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    @Column(name = "view_count")
    private Long viewCount;

    @Column(name = "like_count")
    private Long likeCount;

    @Column(name = "comment_count")
    private Long commentCount;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(length = 20)
    private String duration;

    @Column(name = "collected_at", nullable = false)
    private OffsetDateTime collectedAt;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    @Builder
    private TrendingVideo(String videoId, String title, String channelTitle,
                          String countryCode, Integer categoryId, Integer rankPosition,
                          Long viewCount, Long likeCount, Long commentCount,
                          OffsetDateTime publishedAt, String thumbnailUrl,
                          String duration, OffsetDateTime collectedAt) {
        this.videoId = videoId;
        this.title = title;
        this.channelTitle = channelTitle;
        this.countryCode = countryCode;
        this.categoryId = categoryId;
        this.rankPosition = rankPosition;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.publishedAt = publishedAt;
        this.thumbnailUrl = thumbnailUrl;
        this.duration = duration;
        this.collectedAt = collectedAt;
    }
}
