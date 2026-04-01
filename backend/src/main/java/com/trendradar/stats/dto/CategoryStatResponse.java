package com.trendradar.stats.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryStatResponse {

    private final Integer categoryId;
    private final String categoryName;
    private final int videoCount;
    private final long totalViews;
    private final double avgViews;
    private final double percentage;
}
