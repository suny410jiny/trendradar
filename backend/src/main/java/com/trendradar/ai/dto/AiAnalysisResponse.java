package com.trendradar.ai.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class AiAnalysisResponse {

    private final String analysisType;
    private final String targetId;
    private final String content;
    private final String modelUsed;
    private final OffsetDateTime createdAt;
    private final boolean fromCache;
}
