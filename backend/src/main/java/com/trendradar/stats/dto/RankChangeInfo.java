package com.trendradar.stats.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RankChangeInfo {

    private final String videoId;
    private final Integer currentRank;
    private final Integer previousRank;   // null이면 신규진입
    private final Integer rankChange;     // 양수=상승, 음수=하락, null=신규
    private final String changeType;      // "UP", "DOWN", "SAME", "NEW", "RETURN"

    public static RankChangeInfo newEntry(String videoId, int currentRank) {
        return RankChangeInfo.builder()
                .videoId(videoId)
                .currentRank(currentRank)
                .previousRank(null)
                .rankChange(null)
                .changeType("NEW")
                .build();
    }

    public static RankChangeInfo calculate(String videoId, int currentRank, int previousRank) {
        int change = previousRank - currentRank; // 양수 = 순위 상승
        String type;
        if (change > 0) {
            type = "UP";
        } else if (change < 0) {
            type = "DOWN";
        } else {
            type = "SAME";
        }

        return RankChangeInfo.builder()
                .videoId(videoId)
                .currentRank(currentRank)
                .previousRank(previousRank)
                .rankChange(change)
                .changeType(type)
                .build();
    }
}
