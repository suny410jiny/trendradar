package com.trendradar.stats.controller;

import com.trendradar.common.response.ApiResponse;
import com.trendradar.stats.dto.RankChangeInfo;
import com.trendradar.stats.dto.StatsOverviewResponse;
import com.trendradar.stats.dto.TrendingKeywordResponse;
import com.trendradar.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * 대시보드 통합 통계
     * GET /api/v1/stats/overview?country=KR
     */
    @GetMapping("/overview")
    public ApiResponse<StatsOverviewResponse> getOverview(@RequestParam String country) {
        return ApiResponse.of(statsService.getOverview(country));
    }

    /**
     * 순위 변동 정보
     * GET /api/v1/stats/rank-changes?country=KR
     */
    @GetMapping("/rank-changes")
    public ApiResponse<Map<String, RankChangeInfo>> getRankChanges(@RequestParam String country) {
        return ApiResponse.of(statsService.getRankChanges(country));
    }

    /**
     * 트렌딩 키워드 (YouTube 원본 태그 기반)
     * GET /api/v1/stats/keywords?country=KR&limit=30
     */
    @GetMapping("/keywords")
    public ApiResponse<List<TrendingKeywordResponse>> getTrendingKeywords(
            @RequestParam String country,
            @RequestParam(defaultValue = "30") int limit) {
        return ApiResponse.of(statsService.getTrendingKeywords(country, Math.min(limit, 100)));
    }

    /**
     * Shorts vs 일반 영상 통계
     * GET /api/v1/stats/shorts?country=KR
     */
    @GetMapping("/shorts")
    public ApiResponse<Map<String, Object>> getShortsStats(@RequestParam String country) {
        return ApiResponse.of(statsService.getShortsStats(country));
    }
}
