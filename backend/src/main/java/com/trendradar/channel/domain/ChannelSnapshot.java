package com.trendradar.channel.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "channel_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_id", nullable = false, length = 30)
    private String channelId;

    @Column(name = "subscriber_count")
    private Long subscriberCount;

    @Column(name = "video_count")
    private Long videoCount;

    @Column(name = "total_view_count")
    private Long totalViewCount;

    @Column(name = "trending_video_count")
    private Integer trendingVideoCount;

    @Column(name = "snapshot_at", nullable = false)
    private OffsetDateTime snapshotAt;

    @Builder
    private ChannelSnapshot(String channelId, Long subscriberCount,
                            Long videoCount, Long totalViewCount,
                            Integer trendingVideoCount, OffsetDateTime snapshotAt) {
        this.channelId = channelId;
        this.subscriberCount = subscriberCount;
        this.videoCount = videoCount;
        this.totalViewCount = totalViewCount;
        this.trendingVideoCount = trendingVideoCount;
        this.snapshotAt = snapshotAt;
    }
}
