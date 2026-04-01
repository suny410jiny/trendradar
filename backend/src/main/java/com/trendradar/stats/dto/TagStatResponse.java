package com.trendradar.stats.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TagStatResponse {

    private final String tagType;
    private final String tagLabel;
    private final int videoCount;
    private final double percentage;
}
