package com.trendradar.crossborder.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OpportunityResponse {
    private final String keyword;
    private final List<String> trendingCountries;
    private final String targetCountry;
    private final int totalVideoCount;
    private final long totalViews;
}
