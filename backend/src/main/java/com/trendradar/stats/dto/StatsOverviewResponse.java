package com.trendradar.stats.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class StatsOverviewResponse {

    private final String countryCode;
    private final String countryName;

    // KPI 메트릭
    private final int totalVideos;
    private final int newEntryCount;
    private final int surgeCount;
    private final double avgEngagementRate;
    private final long totalViews;

    // 24시간 내 고유 영상 수 (TOP 50 순환으로 실제 더 많음)
    private final int uniqueVideos24h;

    // 분포 데이터
    private final List<CategoryStatResponse> categoryDistribution;
    private final List<TagStatResponse> tagDistribution;
    private final List<DemographicStatResponse> demographicDistribution;

    private final OffsetDateTime generatedAt;
}
