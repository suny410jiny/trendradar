package com.trendradar.trending.controller;

import com.trendradar.common.response.ApiResponse;
import com.trendradar.trending.domain.YouTubeCategory;
import com.trendradar.trending.dto.*;
import com.trendradar.trending.service.TrendingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TrendingController {

    private final TrendingService trendingService;

    @GetMapping("/trending")
    public ApiResponse<List<TrendingVideoResponse>> getTrending(
            @RequestParam String country,
            @RequestParam(required = false) Integer category,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "10") int limit) {

        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return ApiResponse.of(trendingService.getTrending(country, category, tag, safeLimit));
    }

    @GetMapping("/trending/{videoId}/snapshots")
    public ApiResponse<List<ViewSnapshotResponse>> getSnapshots(
            @PathVariable String videoId) {
        return ApiResponse.of(trendingService.getSnapshots(videoId));
    }

    @GetMapping("/countries")
    public ApiResponse<List<CountryResponse>> getCountries() {
        return ApiResponse.of(trendingService.getCountries());
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> categories = YouTubeCategory.all().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName()))
                .toList();
        return ApiResponse.of(categories);
    }
}
