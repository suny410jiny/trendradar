package com.trendradar.trending.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class BriefingResponse {

    private final String country;
    private final String countryName;
    private final String summary;
    private final List<TrendingVideoResponse> topVideos;
    private final OffsetDateTime generatedAt;
}
