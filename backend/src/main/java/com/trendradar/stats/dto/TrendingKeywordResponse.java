package com.trendradar.stats.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrendingKeywordResponse {

    private final String keyword;
    private final int count;        // 해당 키워드를 가진 영상 수
    private final double percentage;
}
