package com.trendradar.trending.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "view_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ViewSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false, length = 20)
    private String videoId;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "snapshot_at", nullable = false)
    private OffsetDateTime snapshotAt;

    @Builder
    private ViewSnapshot(String videoId, Long viewCount, OffsetDateTime snapshotAt) {
        this.videoId = videoId;
        this.viewCount = viewCount;
        this.snapshotAt = snapshotAt;
    }
}
