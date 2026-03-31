package com.trendradar.trending.dto;

import com.trendradar.trending.domain.ViewSnapshot;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class ViewSnapshotResponse {

    private final String videoId;
    private final Long viewCount;
    private final OffsetDateTime snapshotAt;

    public static ViewSnapshotResponse from(ViewSnapshot snapshot) {
        return ViewSnapshotResponse.builder()
                .videoId(snapshot.getVideoId())
                .viewCount(snapshot.getViewCount())
                .snapshotAt(snapshot.getSnapshotAt())
                .build();
    }
}
