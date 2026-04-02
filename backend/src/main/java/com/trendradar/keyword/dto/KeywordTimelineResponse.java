package com.trendradar.keyword.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class KeywordTimelineResponse {
    private final String keyword;
    private final String periodType;
    private final OffsetDateTime periodStart;
    private final int videoCount;
    private final long totalViews;
}
