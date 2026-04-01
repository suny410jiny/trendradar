package com.trendradar.channel.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChannelRankingResponse {

    private final String channelId;
    private final String title;
    private final String thumbnailUrl;
    private final Long subscriberCount;
    private final Long totalViewCount;
    private final double surgeScore;
    private final String grade;
    private final String gradeLabel;
    private final boolean darkhorse;
    private final int trendingVideoCount;
    private final double burstRatio;
}
