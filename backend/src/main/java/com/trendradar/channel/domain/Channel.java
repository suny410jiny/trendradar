package com.trendradar.channel.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "channels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel {

    @Id
    @Column(name = "channel_id", length = 30)
    private String channelId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "subscriber_count")
    private Long subscriberCount;

    @Column(name = "video_count")
    private Long videoCount;

    @Column(name = "total_view_count")
    private Long totalViewCount;

    @Column(name = "first_seen_at")
    private OffsetDateTime firstSeenAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.firstSeenAt == null) {
            this.firstSeenAt = this.createdAt;
        }
    }

    @Builder
    private Channel(String channelId, String title, String thumbnailUrl,
                    Long subscriberCount, Long videoCount, Long totalViewCount,
                    OffsetDateTime firstSeenAt) {
        this.channelId = channelId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.subscriberCount = subscriberCount;
        this.videoCount = videoCount;
        this.totalViewCount = totalViewCount;
        this.firstSeenAt = firstSeenAt;
    }

    public void updateStats(String title, String thumbnailUrl,
                            Long subscriberCount, Long videoCount, Long totalViewCount) {
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.subscriberCount = subscriberCount;
        this.videoCount = videoCount;
        this.totalViewCount = totalViewCount;
        this.updatedAt = OffsetDateTime.now();
    }
}
