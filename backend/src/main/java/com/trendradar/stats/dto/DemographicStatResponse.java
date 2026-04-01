package com.trendradar.stats.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DemographicStatResponse {

    private final String ageGroup;        // "10대", "20대", "30대", "40대+"
    private final int videoCount;
    private final double percentage;
    private final List<String> topCategories;  // 해당 연령대의 주요 카테고리
}
