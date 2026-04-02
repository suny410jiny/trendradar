package com.trendradar.channel.dto;

import com.trendradar.ai.dto.AiAnalysisResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class ChannelDetailResponse {
    private final String channelId;
    private final String title;
    private final String thumbnailUrl;
    private final Long subscriberCount;
    private final Long videoCount;
    private final Long totalViewCount;
    private final double surgeScore;
    private final String grade;
    private final String gradeLabel;
    private final boolean darkhorse;
    private final OffsetDateTime firstSeenAt;
    private final AiAnalysisResponse aiAnalysis;  // nullable — may not exist yet
}
