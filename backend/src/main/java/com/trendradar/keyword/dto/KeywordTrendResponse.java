package com.trendradar.keyword.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KeywordTrendResponse {
    private final String keyword;
    private final int videoCount;
    private final long totalViews;
    private final double avgEngagement;
    private final double keywordScore;  // frequency_norm * 0.5 + impact_norm * 0.5
}
